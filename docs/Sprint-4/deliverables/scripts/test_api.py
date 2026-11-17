"""Live integration checks against the running MySQL-backed application. Standard Python only."""
import json,urllib.request,urllib.error,urllib.parse,http.cookiejar,os
BASE=os.getenv('CAREERUP_URL','http://127.0.0.1:8080')
checks=0
def check(value,label):
    global checks
    assert value,label
    checks+=1
    print('PASS',label)
class Client:
    def __init__(self):self.jar=http.cookiejar.CookieJar();self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
    def call(self,path,data=None,csrf=True):
        headers={}
        if data is not None:
            headers['Content-Type']='application/x-www-form-urlencoded'
            if csrf:
                _,t=self.call('/api/auth/csrf');headers[t['headerName']]=t['token']
            data=urllib.parse.urlencode(data).encode()
        try:
            with self.opener.open(urllib.request.Request(BASE+path,data=data,headers=headers)) as r:
                raw=r.read();return r.status,json.loads(raw) if raw else None
        except urllib.error.HTTPError as e:
            raw=e.read()
            try:v=json.loads(raw)
            except:v=raw.decode()
            return e.code,v
    def login(self,name,password='CareerUp4!'):return self.call('/api/auth/login',{'email':name+'@careerup.demo','password':password})
routes={'student':'/api/student/feed','recruiter':'/api/recruiter/students','company':'/api/company/requests','advisor':'/api/advisor/students','admin':'/api/admin/users'}
roles={'student':'STUDENT','recruiter':'RECRUITER','company':'COMPANY_REP','advisor':'CAREER_ADVISOR','admin':'ADMIN'}
def main():
    c=Client();check(c.call('/api/health')[1]['database']=='UP','MySQL health')
    for route in routes.values():check(c.call(route)[0]==401,'Anonymous denied '+route)
    check(c.login('student','wrong-password')[0]==401,'Invalid password rejected')
    check(c.login('missing')[0]==401,'Unknown email rejected')
    check(c.login('inactive')[0]==401,'Inactive account rejected')
    check(c.call('/api/auth/login',{'email':'student@careerup.demo','password':'CareerUp4!'},csrf=False)[0]==403,'Login requires CSRF token')
    clients={}
    for name,role in roles.items():
        c=Client();clients[name]=c
        check(c.login(name)[0]==200,name+' signs in')
        check(c.call('/api/auth/me')[1]['role']==role,name+' role comes from database')
        for other,route in routes.items():
            status,data=c.call(route);check(status==(200 if name==other else 403),name+' authorization '+other)
            if name==other:
                check(data['total']>0 and len(data['items'])>0,name+' retrieves seeded database rows')
                check(all('password_hash' not in row and 'phone' not in row for row in data['items']),name+' does not expose password or private phone')
    c=clients['recruiter']
    r=c.call('/api/recruiter/students?major=5&skill=4&year=2026')[1]
    check([x['id'] for x in r['items']]==[9101],'Combined major/skill/year selects Nia')
    check(c.call('/api/recruiter/students?q=zzzz-no-match')[1]['total']==0,'Search empty state')
    check(c.call('/api/recruiter/students?q='+urllib.parse.quote("' OR 1=1 --"))[1]['total']==0,'Search parameter cannot inject SQL')
    check(c.call('/api/recruiter/filters')[1]['majors'],'Filter options read from database')
    all_ids=[];p=0
    while True:
        r=clients['admin'].call('/api/admin/users?size=3&page='+str(p))[1];all_ids += [x['id'] for x in r['items']]
        if (p+1)*3>=r['total']:break
        p+=1
    check(len(all_ids)==r['total'] and len(set(all_ids))==r['total'],'Pagination traverses all users without duplicates')
    check(all(x['role']=='STUDENT' for x in clients['admin'].call('/api/admin/users?role=STUDENT')[1]['items']),'Admin role filter')
    check(clients['admin'].call('/api/admin/users?q=admin%40careerup.demo')[1]['total']==1,'Admin email search')
    c=clients['advisor'];r=c.call('/api/advisor/students')[1]
    check({x['id'] for x in r['items']}=={9101,102},'Advisor sees only active assignments')
    check(c.call('/api/advisor/students/9107')[0]==403,'Advisor cannot open unassigned student')
    status,r=c.call('/api/advisor/students/9101');check(status==200 and r['portfolio']['total']>=2,'Advisor selected profile includes published portfolio')
    check('email' not in r['student'] and 'phone' not in r['student'],'Advisor profile uses limited fields')
    first=clients['company'].call('/api/company/requests')[1]
    other=Client();check(other.login('other-company')[0]==200,'Second company signs in')
    second=other.call('/api/company/requests')[1]
    check(all(x['company']=='Delta Air Lines' for x in first['items']),'Delta representative sees Delta requests only')
    check(all(x['company']=='Truist Financial' for x in second['items']),'Truist representative sees Truist requests only')
    check(not ({x['id'] for x in first['items']}&{x['id'] for x in second['items']}),'Company request sets are disjoint')
    check(all(x['company']=='Delta Air Lines' for x in clients['company'].call('/api/company/requests?company_id=2')[1]['items']),'Client company ID cannot broaden scope')
    check(all(x['verification_status']=='PENDING' for x in clients['company'].call('/api/company/requests?status=PENDING')[1]['items']),'Request status filter')
    feed=clients['student'].call('/api/student/feed?size=50')[1]
    check({9201,9202,9203,9204,9205}.issubset({x['id'] for x in feed['items']}),'Discovery includes actual student project IDs')
    check(1001 not in {x['id'] for x in feed['items']},'Non-student author posts are excluded')
    check(clients['student'].call('/api/student/feed?q=Campus%20Companion')[1]['total']==1,'Feed project search')
    for term in ['Campus','Nia','Python','Figma','Computer Information Systems']:
        result=clients['student'].call('/api/student/feed?q='+urllib.parse.quote(term))[1]
        check(result['total']>0,'Student search matches '+term)
    check(clients['student'].call('/api/student/feed?q=zzzz-no-project-match')[1]['total']==0,'Unmatched student search is empty')
    check(clients['student'].call('/api/student/feed')[1]['total']>=5,'Clearing student search restores published posts')
    c=clients['student'];check(c.call('/api/auth/logout',{})[0]==204,'Logout succeeds')
    check(c.call('/api/student/feed')[0]==401,'Logged-out session cannot retrieve protected data')
    print('ALL',checks,'LIVE API CHECKS PASSED')

if __name__=='__main__':main()
