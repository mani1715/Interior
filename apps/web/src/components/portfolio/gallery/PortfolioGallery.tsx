'use client';
/* eslint-disable @next/next/no-img-element -- gallery uses the supplied watermarked derivatives. */
import {useEffect,useId,useRef,useState,type ImgHTMLAttributes} from 'react';
import {createPortal} from 'react-dom';
import styles from './PortfolioGallery.module.css';

export type GalleryPhotoData={id:string;src:string;alt?:string|null;caption?:string|null;kind?:string};
export type GalleryFolder={id:string;name:string;description?:string;photos:GalleryPhotoData[]};
export function gallerySource(value:string){
  if(/[\s\\\u0000-\u001f]/.test(value))return;
  if(value.startsWith('/api/v1/media/public/')) {
    const path=new URL(value,'https://gallery.invalid').pathname;
    return path.startsWith('/api/v1/media/public/')&&!/%2e|%2f|%5c/i.test(path)?value:undefined;
  }
  try {const u=new URL(value);return u.protocol==='https:'&&!u.username&&!u.password?value:undefined;}catch{return undefined;}
}
export function GalleryViewer({folders,initialFolder,initialPhoto=0,onClose}:{folders:GalleryFolder[];initialFolder:string;initialPhoto?:number;onClose:()=>void}) {
  const [folderId,setFolderId]=useState(initialFolder),[index,setIndex]=useState(initialPhoto),[overview,setOverview]=useState(false);
  const dialog=useRef<HTMLDialogElement>(null),close=useRef(onClose),title=useId(),touch=useRef<number|null>(null);
  close.current=onClose;
  const folder=folders.find(f=>f.id===folderId)||folders[0];
  const photo=folder?.photos[index]||folder?.photos[0];
  const move=(delta:number)=>setIndex(i=>(i+delta+folder.photos.length)%folder.photos.length);
  useEffect(()=>{
    const before=document.activeElement as HTMLElement|null,overflow=document.body.style.overflow;
    dialog.current?.showModal();document.body.style.overflow='hidden';
    return ()=>{document.body.style.overflow=overflow;before?.focus();};
  },[]);
  if(!folder||!photo)return null;
  return createPortal(<dialog ref={dialog} className={styles.viewer} aria-labelledby={title} onCancel={e=>{e.preventDefault();close.current();}}
    onKeyDown={e=>{if(e.key==='ArrowRight'){e.preventDefault();move(1);}if(e.key==='ArrowLeft'){e.preventDefault();move(-1);}}}>
    <header className={styles.viewerHeader}><div><span className={styles.eyebrow}>THE GALLERY</span><h2 id={title}>{folder.name}</h2></div><button type="button" onClick={onClose} aria-label="Close gallery">Close ×</button></header>
    <div className={styles.layout}><nav className={styles.folders} aria-label="Gallery folders"><span className={styles.eyebrow}>COLLECTIONS / {String(folders.length).padStart(2,'0')}</span>{folders.map(f=><button type="button" key={f.id} aria-label={`${f.name}, ${f.photos.length} ${f.photos.length===1?'photo':'photos'}`} aria-pressed={f.id===folder.id} onClick={()=>{setFolderId(f.id);setIndex(0);setOverview(false);}}><span>{f.name}</span><small>{f.photos.length}</small></button>)}</nav>
    <div className={styles.stage}><div className={styles.toolbar}><span aria-live="polite">{String(index+1).padStart(2,'0')} / {String(folder.photos.length).padStart(2,'0')}</span><button type="button" onClick={()=>setOverview(v=>!v)}>{overview?'Full photo':'Contact sheet'}</button></div>
    {overview?<div className={styles.contactSheet}>{folder.photos.map((p,i)=><button type="button" key={p.id} aria-label={`View photo ${i+1}`} onClick={()=>{setIndex(i);setOverview(false);}}><img src={p.src} alt={p.alt||''}/><span>{String(i+1).padStart(2,'0')}</span></button>)}</div>:<div className={styles.imageStage} onTouchStart={e=>{touch.current=e.touches[0].clientX;}} onTouchEnd={e=>{if(touch.current!==null){const distance=e.changedTouches[0].clientX-touch.current;if(Math.abs(distance)>50)move(distance<0?1:-1);touch.current=null;}}}>
      <img key={photo.id} src={photo.src} alt={photo.alt||folder.name} onError={e=>{e.currentTarget.alt='This photo is temporarily unavailable';}}/>
      {folder.photos.length>1&&<><button type="button" className={styles.previous} aria-label="Previous photo" onClick={()=>move(-1)}>←</button><button type="button" className={styles.next} aria-label="Next photo" onClick={()=>move(1)}>→</button></>}
    </div>}
    <div className={styles.caption}><span>{photo.kind==='AI_CONCEPT'?'AI concept visualization':photo.caption||photo.alt||folder.name}</span><span>{folder.description}</span></div>
    <div className={styles.filmstrip} aria-label="Photo thumbnails">{folder.photos.map((p,i)=><button type="button" key={p.id} aria-label={`Photo ${i+1}`} aria-pressed={index===i} onClick={()=>{setIndex(i);setOverview(false);}}><img src={p.src} alt=""/></button>)}</div>
    </div></div>
  </dialog>,document.body);
}

export function PortfolioGallery({folders}:{folders:GalleryFolder[]}) {
  const [selected,setSelected]=useState<string|null>(null);
  const safe=folders.map(f=>({...f,photos:f.photos.filter(p=>gallerySource(p.src))})).filter(f=>f.photos.length);
  if(!safe.length)return null;
  return <section className={styles.gallery} aria-label="Portfolio gallery"><div className={styles.intro}><div><span className={styles.eyebrow}>SPACES, IN DETAIL</span><h2>Inside the work.</h2></div><p>Explore the materials, details and spaces.<br/>Open a folder to take a closer look.</p></div><div className={styles.folderGrid}>{safe.map((f,i)=><button type="button" className={styles.folderCard} key={f.id} onClick={()=>setSelected(f.id)}><div className={styles.cover}><img src={f.photos[0].src} alt={f.photos[0].alt||f.name} loading="lazy"/><span className={styles.open}>Open folder ↗</span></div><div className={styles.folderMeta}><span className={styles.number}>{String(i+1).padStart(2,'0')}</span><div><h3>{f.name}</h3><p>{f.photos.length} {f.photos.length===1?'photograph':'photographs'}</p></div><span aria-hidden="true">↗</span></div></button>)}</div>{selected&&<GalleryViewer folders={safe} initialFolder={selected} onClose={()=>setSelected(null)}/>}</section>;
}

/** Supplied template images remain normal images, with a native keyboard-accessible opener. */
export function GalleryPhoto(props:ImgHTMLAttributes<HTMLImageElement>) {
  const [state,setState]=useState<{folders:GalleryFolder[];folder:string;index:number}|null>(null);
  function open(button:HTMLButtonElement){
    const root=button.closest('main')||button.parentElement!;
    const groups=new Map<Element,GalleryFolder>();let selected='';let index=0;
    root.querySelectorAll<HTMLButtonElement>('button[data-gallery-photo]').forEach((b,i)=>{
      const img=b.querySelector('img');if(!img)return;
      const group=b.closest('article')||b.closest('section')||root;
      if(!groups.has(group))groups.set(group,{id:`group-${i}`,name:group.querySelector('h3,h2')?.textContent||'Portfolio photographs',photos:[]});
      const f=groups.get(group)!;
      if(b===button){selected=f.id;index=f.photos.length;}
      f.photos.push({id:String(i),src:img.currentSrc||img.src,alt:img.alt,caption:b.closest('figure')?.querySelector('figcaption')?.textContent});
    });
    setState({folders:[...groups.values()],folder:selected,index});
  }
  return <><button className={styles.photoButton} type="button" data-gallery-photo aria-label={`Open photo: ${props.alt||'Portfolio image'}`} onClick={e=>open(e.currentTarget)}><img {...props} alt={props.alt||''}/><span className={styles.photoHint} aria-hidden="true">View photo ↗</span></button>{state&&<GalleryViewer folders={state.folders} initialFolder={state.folder} initialPhoto={state.index} onClose={()=>setState(null)}/>}</>;
}
