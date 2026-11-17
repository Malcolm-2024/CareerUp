"""Verify retrieval responds to real MySQL changes; uses a temporary isolated fixture."""
import os,subprocess
from test_api import Client,check
mysql=os.getenv('MYSQL_EXE','C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe')
def sql(text):
    subprocess.run([mysql,'--host=127.0.0.1','--port='+os.getenv('CAREERUP_DB_PORT','3307'),'-u','root','careerup_sprint4'],input=text.encode(),check=True,stdout=subprocess.DEVNULL)
c=Client();assert c.login('student')[0]==200
try:
    sql("INSERT INTO posts(id,author_id,title,status) VALUES(990099,9101,'Sprint4 live database proof fixture','PUBLISHED');")
    check(c.call('/api/student/feed?q=Sprint4%20live%20database%20proof%20fixture')[1]['total']==1,'Inserted MySQL row appears immediately in API')
    sql("UPDATE posts SET status='HIDDEN' WHERE id=990099;")
    check(c.call('/api/student/feed?q=Sprint4%20live%20database%20proof%20fixture')[1]['total']==0,'Hidden MySQL row disappears from discovery')
    sql("UPDATE posts SET status='DRAFT' WHERE id=990099;")
    check(c.call('/api/student/feed?q=Sprint4%20live%20database%20proof%20fixture')[1]['total']==0,'Drafts stay out of discovery')
finally:
    sql('DELETE FROM posts WHERE id=990099;')
print('Live database fixture removed; original sample data retained.')
