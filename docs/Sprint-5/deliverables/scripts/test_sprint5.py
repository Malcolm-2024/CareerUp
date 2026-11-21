"""Live API + direct MySQL checks. Run only against the isolated Sprint 5 demo DB.
MYSQL_HOME points to MySQL's tools; CAREERUP_URL defaults to localhost:8085.
No password is printed. Test rows remain available for persistence inspection.
"""
import concurrent.futures, http.cookiejar, json, os, pathlib, subprocess, time, urllib.error, urllib.parse, urllib.request, uuid

ROOT=pathlib.Path(__file__).resolve().parents[1]
BASE=os.environ.get('CAREERUP_URL','http://127.0.0.1:8085')
COUNT=0
def check(value,label):
    global COUNT
    assert value,label
    COUNT+=1
    print('PASS',label,flush=True)

class Client:
    def __init__(self):
        self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def call(self,path,body=None,key=None,csrf=True,form=False):
        headers={}
        if body is not None:
            if csrf:
                _,token=self.call('/api/auth/csrf');headers[token['headerName']]=token['token']
            headers['Content-Type']='application/x-www-form-urlencoded' if form else 'application/json'
            if key is not False:headers['Idempotency-Key']=key or str(uuid.uuid4())
            body=urllib.parse.urlencode(body).encode() if form else json.dumps(body).encode()
        try:
            with self.opener.open(urllib.request.Request(BASE+path,data=body,headers=headers),timeout=30) as response:
                raw=response.read();return response.status,json.loads(raw) if raw else None
        except urllib.error.HTTPError as response:
            raw=response.read()
            try:data=json.loads(raw)
            except Exception:data=raw.decode()
            return response.code,data
    def login(self,name,password='CareerUp4!'):
        return self.call('/api/auth/login',{'email':name+'@careerup.demo','password':password},form=True)

def sql(query):
    settings={}
    for line in (ROOT/'.local/IntelliJ MySQL Connection.txt').read_text().splitlines():
        if ': ' in line:
            k,v=line.split(': ',1);settings[k]=v
    assert settings['Database']=='careerup_sprint5','Refusing to test against a non-Sprint-5 database'
    tool=pathlib.Path(os.environ['MYSQL_HOME'])/'bin'/('mysql.exe' if os.name=='nt' else 'mysql')
    env=os.environ.copy();env['MYSQL_PWD']=settings['Password']
    command=[str(tool),'--no-defaults','--host='+settings['Host'],'--port='+settings['Port'],'-u',settings['User'],'-N','--batch','--default-character-set=utf8mb4',settings['Database']]
    # Windows can occasionally return an empty captured CLI stream. COUNT always
    # returns a row, so retry only that read; never replay an INSERT or UPDATE.
    for attempt in range(3):
        result=subprocess.run(command,input=query,text=True,capture_output=True,env=env,check=True)
        value=result.stdout.strip()
        if value or not query.lstrip().upper().startswith('SELECT COUNT('):return value
        print('NOTE: MySQL CLI returned no COUNT output; repeating the read.',flush=True)
        time.sleep(0.1)
    raise RuntimeError('MySQL CLI returned no COUNT result after three reads.')

def main():
    clients={r:Client() for r in ['admin','student','recruiter','company','advisor','other-company','malik']}
    check(Client().login('student','wrong')[0]==401,'Invalid login rejected')
    for role,c in clients.items():check(c.login(role)[0]==200,role+' signs in')
    routes={'admin':'/api/admin/users','student':'/api/student/feed','recruiter':'/api/recruiter/students','company':'/api/company/requests','advisor':'/api/advisor/students'}
    writes={'admin':('/api/admin/users/9108/status',{'status':'ACTIVE','expectedStatus':'INACTIVE'}),'student':('/api/student/posts',{'title':'Test','description':'Test','url':'https://example.com/test.png','mediaType':'IMAGE'}),'recruiter':('/api/recruiter/jobs',{'title':'Test','description':'Test','location':'Remote','employmentType':'INTERNSHIP'}),'company':('/api/company/requests/9203/decision',{'decision':'APPROVED'}),'advisor':('/api/advisor/students/9101/feedback',{'postId':'9201','content':'Test'})}
    for role,route in routes.items():
        check(Client().call(route)[0]==401,'Anonymous cannot retrieve '+role)
        for other,c in clients.items():
            if other in routes:
                check(c.call(route)[0]==(200 if other==role else 403),other+' retrieval authorization '+role)
        path,data=writes[role]
        check(Client().call(path,data)[0]==401,'Anonymous write denied '+role)
        for other,c in clients.items():
            if other in routes and other!=role:check(c.call(path,data)[0]==403,other+' cannot write '+role)

    # ADMIN: persistence, stale update conflict, login and existing-session invalidation.
    admin=clients['admin'];old=sql('SELECT status FROM users WHERE id=9107')
    check(admin.call('/api/admin/users/9107/status',{'status':'INACTIVE','expectedStatus':old})[0]==200,'Administrator deactivates account')
    check(sql('SELECT status FROM users WHERE id=9107')=='INACTIVE','Deactivation persisted in MySQL')
    check(Client().login('malik')[0]==401,'Deactivated account cannot sign in')
    check(clients['malik'].call('/api/student/feed')[0]==401,'Existing session loses access after deactivation')
    check(admin.call('/api/admin/users/9107/status',{'status':'INACTIVE','expectedStatus':'ACTIVE'})[0]==409,'Stale account modification rejected')
    check(admin.call('/api/admin/users/9107/status',{'status':'ACTIVE','expectedStatus':'INACTIVE'})[0]==200,'Administrator reactivates account')
    check(sql('SELECT status FROM users WHERE id=9107')=='ACTIVE','Reactivation persisted in MySQL')
    check(Client().login('malik')[0]==200,'Reactivated account can sign in')
    check(admin.call('/api/admin/users/9105/status',{'status':'INACTIVE','expectedStatus':'ACTIVE'})[0]==400,'Administrator cannot deactivate self')
    check(admin.call('/api/admin/users/9107/status',{'status':'BANNED','expectedStatus':'ACTIVE'})[0]==400,'Invalid account status rejected')

    student=clients['student'];stamp=uuid.uuid4().hex[:10]
    body={'title':'Sprint 5 integration '+stamp,'description':'A real portfolio record for live testing.','url':'https://example.com/portfolio.png','mediaType':'IMAGE'}
    key=str(uuid.uuid4());status,post=student.call('/api/student/posts',body,key)
    check(status==200,'Student publishes portfolio post')
    pid=post['id'];check(sql(f"SELECT CONCAT(author_id,':',status) FROM posts WHERE id={pid}")=='9101:PUBLISHED','Post ownership and publication persisted')
    check(sql(f'SELECT COUNT(*) FROM post_media WHERE post_id={pid}')=='1','Post media persisted atomically')
    check(student.call('/api/student/posts',body,key)==(status,post),'Retry returns original saved result')
    check(sql("SELECT COUNT(*) FROM posts WHERE title='"+body['title']+"'")=='1','Retry did not create a duplicate')
    changed=dict(body,title='Changed');check(student.call('/api/student/posts',changed,key)[0]==409,'Reusing key with different data rejected')
    check(student.call('/api/student/posts',body,key=False)[0]==400,'Missing submission key rejected')
    check(student.call('/api/student/posts',body,csrf=False)[0]==403,'Writes require CSRF token')
    for bad in [{},dict(body,title='   '),dict(body,title='x'*256),dict(body,url='javascript:alert(1)'),dict(body,mediaType='FILE')]:
        check(student.call('/api/student/posts',bad)[0]==400,'Invalid portfolio submission rejected')
    check(any(x['id']==pid for x in student.call('/api/student/feed?q='+urllib.parse.quote(stamp))[1]['items']),'New post appears in existing discovery retrieval')
    # View your post uses its ID; duplicate titles never open a different project.
    exact=student.call('/api/student/feed?postId='+str(pid))[1]
    check(exact['total']==1 and exact['items'][0]['id']==pid,'View your post retrieves the exact saved ID')
    check(student.call('/api/student/feed?postId=999999999')[1]['total']==0,'Missing saved post returns no unrelated project')
    check(clients['recruiter'].call('/api/student/feed?postId='+str(pid))[0]==403,'Exact post retrieval retains student role authorization')

    recruiter=clients['recruiter'];job={'title':'Internship '+stamp,'description':'Help build accessible student experiences.','location':'Remote','employmentType':'INTERNSHIP','deadline':'2099-12-31'}
    key=str(uuid.uuid4());status,saved=recruiter.call('/api/recruiter/jobs',job,key);check(status==200,'Recruiter publishes opportunity')
    jid=saved['id'];check(sql(f"SELECT CONCAT(posted_by,':',company_id,':',status) FROM jobs WHERE id={jid}")=='9102:1:PUBLISHED','Opportunity persisted with recruiter company ownership')
    check(recruiter.call('/api/recruiter/jobs',job,key)[1]['id']==jid,'Opportunity retry is idempotent')
    check(any(x['id']==jid for x in recruiter.call('/api/recruiter/jobs')[1]),'Opportunity can be retrieved after saving')
    for bad in [{},dict(job,deadline='2020-01-01'),dict(job,deadline='bad-date'),dict(job,employmentType='INVALID')]:check(recruiter.call('/api/recruiter/jobs',bad)[0]==400,'Invalid opportunity rejected')

    # Create isolated request fixtures; never reset Sprint 4 records or demo tables.
    request_ids=[]
    for company in [1,1,2]:
        sql(f"INSERT INTO posts(author_id,company_id,post_type,title,status,verification_status) VALUES(9101,{company},'PORTFOLIO','Verification {stamp}-{company}-{len(request_ids)}','PUBLISHED','PENDING')")
        request_ids.append(int(sql(f"SELECT MAX(id) FROM posts WHERE title LIKE 'Verification {stamp}-%'")))
    rep=clients['company']
    for rid,decision in zip(request_ids,['APPROVED','REJECTED']):
        check(rep.call(f'/api/company/requests/{rid}/decision',{'decision':decision})[0]==200,'Company '+decision.lower()+' verification')
        check(sql(f"SELECT CONCAT(verification_status,':',verified_by,':',verified_at IS NOT NULL) FROM posts WHERE id={rid}")==decision+':9103:1','Verification decision and reviewer persisted')
        check(rep.call(f'/api/company/requests/{rid}/decision',{'decision':decision})[0]==409,'Already reviewed request cannot be overwritten')
    foreign=request_ids[2];check(rep.call(f'/api/company/requests/{foreign}/decision',{'decision':'APPROVED'})[0]==409,'Cannot decide another company request')
    check(sql(f'SELECT verification_status FROM posts WHERE id={foreign}')=='PENDING','Unauthorized decision left database unchanged')
    check(rep.call(f'/api/company/requests/{foreign}/decision',{'decision':'INVALID'})[0]==400,'Invalid decision rejected')

    advisor=clients['advisor'];note={'postId':str(pid),'content':'Clear visual hierarchy. Add a short explanation of the outcomes. '+stamp}
    key=str(uuid.uuid4());status,saved=advisor.call('/api/advisor/students/9101/feedback',note,key);check(status==200,'Advisor saves portfolio feedback')
    nid=saved['id'];check(sql(f"SELECT CONCAT(advisor_id,':',student_id,':',post_id,':',kind) FROM advisor_notes WHERE id={nid}")==f'9104:9101:{pid}:FEEDBACK','Feedback ownership and post persisted')
    check(advisor.call('/api/advisor/students/9101/feedback',note,key)[1]['id']==nid,'Feedback retry is idempotent')
    check(any(x['id']==nid for x in student.call('/api/student/feedback')[1]),'Student can retrieve advisor feedback')
    check(any(x['id']==nid for x in advisor.call('/api/advisor/students/9101/feedback')[1]),'Advisor can retrieve saved feedback')
    check(advisor.call('/api/advisor/students/9107/feedback',note)[0]==403,'Unassigned student feedback blocked')
    check(advisor.call('/api/advisor/students/9101/feedback',dict(note,postId='9202'))[0]==400,'Feedback on another student portfolio blocked')
    check(advisor.call('/api/advisor/students/9101/feedback',dict(note,content='   '))[0]==400,'Blank feedback rejected')
    check(advisor.call('/api/advisor/students/9101/feedback',dict(note,content='x'*5001))[0]==400,'Oversized feedback rejected')
    # Two independent sessions retry the same submission at the same time.
    concurrent_clients=[Client(),Client()]
    for client in concurrent_clients:assert client.login('student')[0]==200
    concurrent_key=str(uuid.uuid4())
    concurrent_body=dict(body,title='Concurrent '+stamp)
    with concurrent.futures.ThreadPoolExecutor(2) as pool:
        results=list(pool.map(lambda c:c.call('/api/student/posts',concurrent_body,concurrent_key),concurrent_clients))
    check(all(r[0]==200 for r in results),'Concurrent retries both succeed')
    check(results[0][1]['id']==results[1][1]['id'],'Concurrent retries return the same ID')
    count=sql("SELECT COUNT(*) FROM posts WHERE title='"+concurrent_body['title']+"'")
    check(count=='1','Concurrent retries create one database record (actual '+repr(count)+')')
    (ROOT/'.local/persistence-check.json').write_text(json.dumps({'post':pid,'job':jid,'note':nid,'retry_key':concurrent_key,'retry_body':concurrent_body,'retry_id':results[0][1]['id']}))
    print('ALL',COUNT,'SPRINT 5 LIVE CHECKS PASSED')

if __name__=='__main__':main()
