import React, {useEffect, useState} from "react";
import {Sun, Moon} from "lucide-react";

// THEME SWITCH: changes colors in place; forms, media and sign-in stay mounted.
export default function ThemeToggle() {
  const [theme,setTheme]=useState(()=>window.CareerUpTheme?.get()||"dark");
  useEffect(()=>{
    const update=()=>setTheme(window.CareerUpTheme?.get()||"dark");
    window.addEventListener("careerup:theme",update);
    update();
    return()=>window.removeEventListener("careerup:theme",update);
  },[]);
  const next=theme==="dark"?"Light":"Dark";
  return <button type="button" className="theme-toggle"
    title={`Switch to ${next.toLowerCase()} mode`}
    aria-label={`Switch to ${next.toLowerCase()} mode`}
    onClick={()=>window.CareerUpTheme?.toggle()}>
    {theme==="dark"?<Sun size={17} aria-hidden="true"/>:<Moon size={17} aria-hidden="true"/>}
    <span>{next} mode</span>
  </button>;
}
