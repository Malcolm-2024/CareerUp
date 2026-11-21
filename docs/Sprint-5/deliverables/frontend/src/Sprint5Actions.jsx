import React, {useEffect, useRef, useState} from "react";
import {fileProblem, upload} from "./requests.js";

const TITLES = {
  ADMIN: "Activate or deactivate an account",
  STUDENT: "Share your next portfolio project",
  RECRUITER: "Publish a job or internship",
  COMPANY_REP: "Review a collaboration request",
  CAREER_ADVISOR: "Give portfolio feedback",
};

// Sprint 5 actions reuse the existing request helper, session, fonts and button styles.
export default function Sprint5Actions({user, api, token, onSaved, onExpired, actionTarget, onBusyChange, onViewPost}) {
  const [open,setOpen]=useState(false);
  // FILE PICKER: files stay local until Publish; nothing is added to Git.
  const [source,setSource]=useState("FILE"), [file,setFile]=useState(null), [progress,setProgress]=useState(0);
  const fileInput=useRef(null);
  const [form,setForm]=useState({mediaType:"IMAGE",employmentType:"INTERNSHIP",status:"INACTIVE",decision:"APPROVED"});
  const [items,setItems]=useState([]), [posts,setPosts]=useState([]), [history,setHistory]=useState([]);
  const [search,setSearch]=useState(""), [selected,setSelected]=useState("");
  const [loading,setLoading]=useState(false), [saving,setSaving]=useState(false), [error,setError]=useState(""), [success,setSuccess]=useState("");
  const [revision,setRevision]=useState(0), [loadError,setLoadError]=useState("");
  const [publishedId,setPublishedId]=useState(null), [profileLoading,setProfileLoading]=useState(false);
  const lock=useRef(false), submission=useRef(null);
  const pendingReview=useRef(null), targetPost=useRef(null), recordSelect=useRef(null);
  const role=user.role, needsSelection=["ADMIN","COMPANY_REP","CAREER_ADVISOR"].includes(role);
  const selectedItem=items.find(x=>String(x.id)===selected);
  function fail(e) {setError(e.message); if(e.status===401) onExpired(e.message);}
  function change(name,value) {setForm(f=>({...f,[name]:value}));submission.current=null;setSuccess("");}

  // REVIEW FROM CARD / ACCOUNT / PORTFOLIO: one form, with the exact clicked record selected.
  useEffect(()=>{
    if(!actionTarget || actionTarget.role!==role)return;
    pendingReview.current=actionTarget.record;targetPost.current=actionTarget.postId||null;
    setSelected("");setPosts([]);setHistory([]);
    submission.current=null;setError("");setSuccess("");setSearch("");setOpen(true);
    setForm(f=>({...f,decision:"APPROVED",status:actionTarget.record.status==="ACTIVE"?"INACTIVE":"ACTIVE",postId:"",content:""}));setRevision(x=>x+1);
    document.getElementById("sprint5-actions")?.scrollIntoView({behavior:"smooth",block:"start"});
  },[actionTarget,role]);
  useEffect(()=>{onBusyChange?.(saving);},[saving,onBusyChange]);
  // SIDEBAR ACTION: opens the form directly; normal browsing starts with it closed.
  useEffect(()=>{
    const openForm=()=>{if(!lock.current)setOpen(true);};
    window.addEventListener("careerup:open-action",openForm);
    return()=>window.removeEventListener("careerup:open-action",openForm);
  },[]);

  // Ignore stale searches/selections when a slower request returns out of order.
  useEffect(()=>{
    if(!open) return;
    let current=true; setLoading(true);setLoadError("");
    const timer=setTimeout(async()=>{
      try {
        let result;
        if(role==="ADMIN") result=await api("/admin/users?size=50&q="+encodeURIComponent(search));
        if(role==="COMPANY_REP") result=await api("/company/requests?size=50&status=PENDING&q="+encodeURIComponent(search));
        if(role==="CAREER_ADVISOR") result=await api("/advisor/students?size=50&q="+encodeURIComponent(search));
        if(role==="RECRUITER") result=await api("/recruiter/jobs");
        if(role==="STUDENT") result=await api("/student/feedback");
        if(!current)return;
        if(needsSelection){
          const target=pendingReview.current;
          // A visible card may be beyond the form's first 50 results. Keep it selectable;
          // the backend still checks ownership and pending status when saving.
          setItems(target&&!result.items.some(x=>String(x.id)===String(target.id))?[target,...result.items]:result.items);
          setSelected(target?String(target.id):"");pendingReview.current=null;
          setPosts([]);setHistory([]);
          if(target)requestAnimationFrame(()=>recordSelect.current?.focus({preventScroll:true}));
        }
        else setHistory(result);
      }catch(e){if(current){setLoadError(e.message);if(e.status===401)onExpired(e.message);}}
      finally{if(current)setLoading(false);}
    },200);
    return()=>{current=false;clearTimeout(timer);};
  },[open,role,search,revision]);
  useEffect(()=>{
    if(role!=="CAREER_ADVISOR" || !selected){setProfileLoading(false);return;}
    let current=true;setProfileLoading(true);setLoadError("");
    Promise.all([api("/advisor/students/"+selected),api("/advisor/students/"+selected+"/feedback")])
      .then(([profile,notes])=>{if(current){
        setPosts(profile.portfolio.items);setHistory(notes);
        if(targetPost.current){
          const post=profile.portfolio.items.find(p=>String(p.id)===String(targetPost.current));
          setForm(f=>({...f,postId:post?String(post.id):""}));targetPost.current=null;
          if(!post)setError("That portfolio post is no longer available. Choose another post.");
        }
      }})
      .catch(e=>{if(current){setLoadError(e.message);if(e.status===401)onExpired(e.message);}})
      .finally(()=>{if(current)setProfileLoading(false);});
    return()=>{current=false;};
  },[selected,role,revision]);

  async function submit(event){
    event.preventDefault(); if(lock.current)return;
    setError("");setSuccess("");
    if(needsSelection&&!selectedItem){setError(({ADMIN:"Choose an account to update.",COMPANY_REP:"Choose a pending collaboration request.",CAREER_ADVISOR:"Choose an assigned student."})[role]);return;}
    if(role==="ADMIN"&&Number(selected)===Number(user.id)){setError("You cannot change your own account status.");return;}
    if(role==="ADMIN"&&selectedItem.status===form.status){setError("Choose a different account status.");return;}
    // BLANK / INVALID FIELDS: specific prompts before any network request.
    const problem = (name,message) => {setError(message);event.currentTarget.elements.namedItem(name)?.focus();};
    if (["STUDENT","RECRUITER"].includes(role)) {
      if (!form.title?.trim()) return problem("title","Enter a title before publishing.");
      if (!form.description?.trim()) return problem("description","Describe your project or opportunity before publishing.");
    }
    if (role==="STUDENT") {
      if (source==="FILE") {const message=fileProblem(file);if(message)return problem("file",message);}
      else {
        try {const url=new URL(form.url||"");if(!["http:","https:"].includes(url.protocol)||url.username||url.password)throw new Error();}
        catch {return problem("url","Enter a valid image or video link beginning with http:// or https://.");}
      }
    }
    if (role==="RECRUITER") {
      if(!form.location?.trim())return problem("location","Enter a location, or enter Remote.");
      const today=new Date(), localDate=`${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,"0")}-${String(today.getDate()).padStart(2,"0")}`;
      if(form.deadline && form.deadline<localDate)return problem("deadline","Choose today or a future application deadline.");
    }
    if(role==="CAREER_ADVISOR") {
      if(!form.postId)return problem("postId","Choose a portfolio post to give feedback on.");
      if(!form.content?.trim())return problem("content","Write your feedback before sending it.");
    }
    let path,body;
    if(role==="ADMIN"){path=`/admin/users/${selected}/status`;body={status:form.status,expectedStatus:selectedItem.status};}
    if(role==="STUDENT"){path="/student/posts";body={title:form.title,description:form.description,url:form.url,mediaType:form.mediaType};}
    if(role==="RECRUITER"){path="/recruiter/jobs";body={title:form.title,description:form.description,location:form.location,employmentType:form.employmentType,deadline:form.deadline||""};}
    if(role==="COMPANY_REP"){path=`/company/requests/${selected}/decision`;body={decision:form.decision};}
    if(role==="CAREER_ADVISOR"){path=`/advisor/students/${selected}/feedback`;body={content:form.content,postId:form.postId};}
    const fromComputer=role==="STUDENT" && source==="FILE";
    if(fromComputer) {path="/student/posts/upload";body={title:form.title,description:form.description,fileName:file.name,size:file.size,modified:file.lastModified};}
    // Keep the same key when retrying identical data after a lost response.
    const fingerprint=JSON.stringify({path,body});
    if(!submission.current||submission.current.fingerprint!==fingerprint)submission.current={fingerprint,key:crypto.randomUUID()};
    lock.current=true;setProgress(0);setSaving(true);
    try{
      const csrf=await token();
      const headers={[csrf.headerName]:csrf.token,"Idempotency-Key":submission.current.key};
      let result;
      if(fromComputer) {
        const data=new FormData();data.append("title",form.title);data.append("description",form.description);data.append("file",file);
        setProgress(0);result=await upload(path,data,headers,setProgress);
      } else result=await api(path,{method:"POST",headers:{"Content-Type":"application/json",...headers},body:JSON.stringify(body)});
      if(role==="STUDENT")setPublishedId(result.id);
      setSuccess(role==="STUDENT"?"Your portfolio post is published.":role==="RECRUITER"?"Your opportunity is published.":role==="CAREER_ADVISOR"?"Feedback saved and shared with the student.":role==="ADMIN"?"Account status updated.":"Verification decision saved.");
      setForm({mediaType:"IMAGE",employmentType:"INTERNSHIP",status:"INACTIVE",decision:"APPROVED"});submission.current=null;
      setFile(null);if(fileInput.current)fileInput.current.value="";
      setSelected("");setRevision(x=>x+1);onSaved();
    }catch(e){fail(e);}finally{lock.current=false;setSaving(false);}
  }
  const field=(name,label,max,type="text",required=true)=><label>{label}<input name={name} type={type} required={required} maxLength={max} value={form[name]||""} onChange={e=>change(name,e.target.value)}/></label>;
  return <section className="sprint5-actions" id="sprint5-actions" aria-label={TITLES[role]}>
    <button className="primary" aria-expanded={open} disabled={saving} onClick={()=>setOpen(x=>!x)}>{open?(role==="STUDENT"?"Close post form":"Close form"):TITLES[role]}</button>
    {/* VIEW YOUR POST: retrieve the saved ID, rather than guessing from its title. */}
    {role==="STUDENT"&&success&&<div className="action-success" role="status">{success} <button type="button" className="profile-button" onClick={()=>{setOpen(false);onViewPost(publishedId);}}>View your post</button></div>}
    {open&&<div className="action-content">
      <h2>{TITLES[role]}</h2>
      <p className="muted">{role==="STUDENT"?"Show your project, skills, and what you learned.":"Changes are saved to your CareerUp database."}</p>
      {role!=="STUDENT"&&success&&<div className="action-success" role="status">{success}</div>}
      {error&&<div className="action-error" role="alert">{error}</div>}
      {loadError&&<div className="action-error" role="alert">{loadError} <button onClick={()=>setRevision(x=>x+1)}>Retry</button></div>}
      {(loading||profileLoading)&&<p role="status">Loading records…</p>}
      <form onSubmit={submit} className="action-form" noValidate>
        <fieldset disabled={saving||profileLoading}>
          {needsSelection&&<>
            <label>Find {role==="ADMIN"?"an account":role==="COMPANY_REP"?"a pending request":"an assigned student"}<input value={search} onChange={e=>{pendingReview.current=null;targetPost.current=null;setSearch(e.target.value);setSelected("");}} placeholder="Search by name…" maxLength={150}/></label>
            <label>{role==="ADMIN"?"Select an account":role==="COMPANY_REP"?"Select a request":"Select a student"}<select ref={recordSelect} required value={selected} disabled={loading} onChange={e=>{targetPost.current=null;setSelected(e.target.value);setPosts([]);setHistory([]);change("postId","");}}>
              <option value="">{items.length?"Choose…":"No matching records"}</option>
              {items.map(x=><option key={x.id} value={x.id}>{x.name||x.title}{role==="ADMIN"?` · ${x.email} · ${x.status}`:role==="COMPANY_REP"?` · ${x.author||x.name||"Portfolio"}`:""}</option>)}
            </select></label>
            {items.length===50&&<small>Showing the first 50 matches. Search to narrow the list.</small>}
          </>}
          {role==="ADMIN"&&<>
            {selectedItem&&<p>Current status: <strong>{selectedItem.status}</strong></p>}
            <label>New status<select value={form.status} onChange={e=>change("status",e.target.value)}><option value="INACTIVE">Deactivate (INACTIVE)</option><option value="ACTIVE">Activate (ACTIVE)</option></select></label>
          </>}
          {["STUDENT","RECRUITER"].includes(role)&&<>
            {field("title","Title",255)}
            <label>Description<textarea name="description" required maxLength={10000} rows={4} value={form.description||""} onChange={e=>change("description",e.target.value)}/></label>
          </>}
          {/* MEDIA SOURCE: uploading from computer is the default; existing URL posts remain supported. */}
          {role==="STUDENT"&&<>
            <label>Picture or video source<select value={source} onChange={e=>{setSource(e.target.value);submission.current=null;setError("");}}><option value="FILE">From my computer</option><option value="LINK">Use an existing link</option></select></label>
            {source==="FILE"?<>
              <label>Choose a picture or video<input ref={fileInput} name="file" type="file" accept=".jpg,.jpeg,.png,.gif,.mp4,.webm" onChange={e=>{
                const chosen=e.target.files?.[0], message=fileProblem(chosen);submission.current=null;setSuccess("");
                if(message){setError(message);setFile(null);e.target.value="";}else{setError("");setFile(chosen);}
              }}/></label>
              <small>JPG, PNG or GIF: up to 10 MB. MP4 or WebM: up to 50 MB.</small>
              {file&&<small className="selected-file">Selected: {file.name} ({(file.size/1024/1024).toFixed(1)} MB)</small>}
              {file&&<FilePreview file={file}/>}
              {saving&&<div className="upload-progress" role="status"><progress max="100" value={progress}/><small>{progress<100?`Uploading… ${progress}%`:"Finishing your post…"}</small></div>}
            </>:<>
              <label>Media type<select value={form.mediaType} onChange={e=>change("mediaType",e.target.value)}><option value="IMAGE">Image</option><option value="VIDEO">Video</option></select></label>
              {field("url","Public image or video URL (https://…)",2048,"url")}
              <small>Use a direct media link you have permission to share.</small>
            </>}
          </>}
          {role==="RECRUITER"&&<>
            {field("location","Location or Remote",255)}
            <label>Opportunity type<select value={form.employmentType} onChange={e=>change("employmentType",e.target.value)}>{["INTERNSHIP","FULL_TIME","PART_TIME","CONTRACT","OTHER"].map(x=><option key={x} value={x}>{x.replaceAll("_"," ")}</option>)}</select></label>
            {field("deadline","Application deadline (optional)",10,"date",false)}
          </>}
          {role==="COMPANY_REP"&&<label>Decision<select value={form.decision} onChange={e=>change("decision",e.target.value)}><option value="APPROVED">Approve verification</option><option value="REJECTED">Reject verification</option></select></label>}
          {role==="CAREER_ADVISOR"&&<>
            <label>Portfolio post<select name="postId" required value={form.postId||""} disabled={loading||!selected} onChange={e=>change("postId",e.target.value)}><option value="">Choose a student's post…</option>{posts.map(p=><option key={p.id} value={p.id}>{p.title}</option>)}</select></label>
            <label>Your feedback<textarea name="content" required maxLength={5000} rows={4} value={form.content||""} onChange={e=>change("content",e.target.value)}/></label>
          </>}
          <button className="primary" type="submit" disabled={saving||loading||profileLoading||!!loadError}>{saving?"Saving…":({ADMIN:"Save account status",STUDENT:"Publish portfolio post",RECRUITER:"Publish opportunity",COMPANY_REP:"Save verification decision",CAREER_ADVISOR:"Send portfolio feedback"})[role]}</button>
        </fieldset>
      </form>
      {history.length>0&&<div className="action-history"><h3>{role==="RECRUITER"?"Your published opportunities":role==="STUDENT"?"Feedback from your advisor":"Your saved feedback"}</h3>{history.map(x=><article key={x.id}><strong>{x.title||x.advisor||"Portfolio feedback"}</strong><p>{x.content||x.description}</p>{x.employment_type&&<small>{x.employment_type.replaceAll("_"," ")} · {x.location}</small>}</article>)}</div>}
    </div>}
  </section>;
}

// LOCAL PREVIEW: browser-only object URL, released when replaced or the form closes.
function FilePreview({file}) {
  const [url,setUrl]=useState(""),[failed,setFailed]=useState(false);
  useEffect(()=>{
    const preview=URL.createObjectURL(file);setUrl(preview);setFailed(false);
    return()=>URL.revokeObjectURL(preview);
  },[file]);
  const video=/\.(mp4|webm)$/i.test(file.name);
  return <div className="upload-preview"><small>Preview before publishing</small>
    {failed?<p role="status">This browser cannot preview this file. Try another supported picture or video.</p>
      :url&&(video?<video controls playsInline preload="metadata" src={url} onError={()=>setFailed(true)} aria-label="Selected video preview"/>
        :<img src={url} alt="Selected picture preview" onError={()=>setFailed(true)}/>)}
  </div>;
}
