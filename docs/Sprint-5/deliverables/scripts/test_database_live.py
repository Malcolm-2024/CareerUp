"""Prove retrieval follows MySQL changes in the isolated Sprint 5 database."""
import uuid,urllib.parse
from test_sprint5 import Client,check,sql
name='Sprint5 database proof '+uuid.uuid4().hex[:12]
sql("INSERT INTO posts(author_id,title,status) VALUES(9101,'"+name+"','PUBLISHED')")
post=int(sql("SELECT MAX(id) FROM posts WHERE title='"+name+"'"))
c=Client();assert c.login('student')[0]==200
route='/api/student/feed?q='+urllib.parse.quote(name)
check(c.call(route)[1]['total']==1,'Inserted MySQL row appears in discovery')
sql(f"UPDATE posts SET status='HIDDEN' WHERE id={post}")
check(c.call(route)[1]['total']==0,'Hidden database row disappears from discovery')
check(c.call('/api/student/feed?postId='+str(post))[1]['total']==0,'Exact post lookup cannot bypass hidden status')
sql(f"UPDATE posts SET status='DRAFT' WHERE id={post}")
check(c.call(route)[1]['total']==0,'Draft database row stays out of discovery')
print('Only the new isolated test fixture was modified; retained as DRAFT for inspection.')
