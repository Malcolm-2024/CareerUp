"""Real multipart upload, MySQL, media access and retry checks; isolated Sprint 5 only."""
import base64, concurrent.futures, hashlib, json, pathlib, urllib.request, urllib.error, uuid
from test_sprint5 import BASE, ROOT, Client, check, sql

PNG=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aU1EAAAAASUVORK5CYII=')

def publish(client, content=PNG, filename='sample.png', title='Upload test', key=None, csrf=True):
    boundary='CareerUp'+uuid.uuid4().hex
    data=b''
    for name,value in [('title',title),('description','A real local upload test.')]:
        data+=f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode()
    data+=f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode()+content+f'\r\n--{boundary}--\r\n'.encode()
    headers={'Content-Type':'multipart/form-data; boundary='+boundary,'Idempotency-Key':key or str(uuid.uuid4())}
    if csrf:
        _,token=client.call('/api/auth/csrf');headers[token['headerName']]=token['token']
    try:
        with client.opener.open(urllib.request.Request(BASE+'/api/student/posts/upload',data=data,headers=headers),timeout=90) as r:
            return r.status,json.loads(r.read())
    except urllib.error.HTTPError as r:
        raw=r.read()
        try:return r.code,json.loads(raw)
        except ValueError:return r.code,raw.decode(errors='replace')

def media(client,url,headers=None):
    try:
        with client.opener.open(urllib.request.Request(BASE+url,headers=headers or {}),timeout=30) as r:return r.status,r.read(),dict(r.headers)
    except urllib.error.HTTPError as r:return r.code,r.read(),dict(r.headers)

def main():
    check(sql('SELECT DATABASE()')=='careerup_sprint5','Uploads use isolated Sprint 5 database')
    student=Client();check(student.login('student')[0]==200,'Upload student signs in')
    key=str(uuid.uuid4());title='Local upload '+uuid.uuid4().hex
    status,result=publish(student,title=title,key=key)
    check(status==200,'Real PNG multipart upload succeeds: '+str(result))
    post=result['id'];url=sql(f'SELECT url FROM post_media WHERE post_id={post}')
    check(url.startswith('/uploads/') and url.endswith('.png'),'MySQL stores stable local media reference')
    check(sql(f'SELECT author_id FROM posts WHERE id={post}')=='9101','Uploaded post belongs to signed-in student')
    check(media(student,url)[1]==PNG,'Uploaded bytes remain readable after request cleanup')
    check(media(student,url,{'Range':'bytes=0-9'})[:2]==(206,PNG[:10]),'Media supports byte-range responses')
    check(media(Client(),url)[0]==401,'Anonymous media access denied')
    again=Client();check(again.login('student')[0]==200 and media(again,url)[1]==PNG,'Media survives signing in again')
    before=len(list((ROOT/'.local/uploads').glob('*')))
    check(publish(student,title=title,key=key)==(200,result),'Upload retry returns original post')
    check(len(list((ROOT/'.local/uploads').glob('*')))==before,'Retry does not leave duplicate media')
    check(publish(student,title=title+' changed',key=key)[0]==409,'Changed upload cannot reuse submission key')
    for content,name,label in [(b'','empty.png','Empty file'),(b'<html>bad</html>','fake.png','Renamed document'),(PNG,'image.exe','Unsupported extension'),(PNG[:20],'broken.png','Damaged image'),(PNG,'valid.png','Blank title')]:
        check(publish(student,content,name,title=' ' if label=='Blank title' else label)[0]==400,label+' rejected')
    check(publish(student,PNG+b'0'*(10*1024*1024),'large.png')[0]==400,'Oversized picture rejected')
    check(publish(student,b'0'*(51*1024*1024),'large.mp4')[0]==413,'Oversized multipart video rejected')
    check(publish(student,csrf=False)[0]==403,'Upload requires CSRF protection')
    for role in ['admin','recruiter','company','advisor']:
        c=Client();assert c.login(role)[0]==200
        check(publish(c)[0]==403,role+' cannot upload student post')
    concurrent_key=str(uuid.uuid4()); concurrent_title='Concurrent upload '+uuid.uuid4().hex
    def attempt(_):
        c=Client();assert c.login('student')[0]==200
        return publish(c,title=concurrent_title,key=concurrent_key)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(attempt,range(2)))
    check(all(s==200 for s,_ in results) and results[0][1]['id']==results[1][1]['id'],'Concurrent upload retries create one post')
    check(sql("SELECT COUNT(*) FROM posts WHERE title='"+concurrent_title+"'")=='1','Concurrent upload has one MySQL row')
    fixtures=ROOT/'.local/test-media'
    for extension in ['mp4','webm']:
        path=fixtures/('flower.'+extension)
        if not path.exists():raise RuntimeError('Missing real video fixture '+str(path))
        data=path.read_bytes();s,r=publish(student,data,path.name)
        check(s==200,'Real '+extension.upper()+' video upload succeeds: '+str(r))
        video_url=sql(f"SELECT url FROM post_media WHERE post_id={r['id']}")
        check(media(student,video_url)[1]==data,extension.upper()+' bytes served intact')
        check(media(student,video_url,{'Range':'bytes=0-99'})[:2]==(206,data[:100]),extension.upper()+' supports seeking ranges')
    (ROOT/'.local/upload-persistence.json').write_text(json.dumps({'url':url,'sha256':hashlib.sha256(PNG).hexdigest(),'key':key,'title':title,'id':post}),encoding='utf-8')
    print('UPLOAD CHECKS PASSED',flush=True)

if __name__=='__main__':main()
