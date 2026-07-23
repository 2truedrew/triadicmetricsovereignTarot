#!/bin/bash
WDIR=/storage/emulated/0/Weave
mkdir -p "$WDIR/html"
FILES=("$WDIR"/html/*.html)
[ ! -f "${FILES[0]}" ] && echo "No HTML in $WDIR/html" && exit 1
{
echo '<!DOCTYPE html><html><head><meta charset=UTF-8><meta name=viewport content="width=device-width,initial-scale=1.0"><title>Apperakadabra Weave</title>'
echo '<style>:root{--p:#6366f1;--b:#2d2d5e;--r:12px}*{margin:0;padding:0;box-sizing:border-box}body{font-family:system-ui,sans-serif;background:#0f0f1a;color:#e2e8f0}.nav{background:#1a1a2e;padding:1rem;display:flex;gap:1rem;flex-wrap:wrap;position:sticky;top:0;z-index:100;border-bottom:1px solid var(--b)}.nav a{color:#a5b4fc;text-decoration:none;padding:.5rem 1rem;border-radius:8px;transition:.2s}.nav a:hover,.nav a.active{background:var(--b);color:#fff}.content{max-width:900px;margin:0 auto;padding:2rem}.card{background:#1a1a2e;border-radius:var(--r);padding:1.5rem;margin:1rem 0;border:1px solid var(--b)}@media(max-width:768px){.content{padding:1rem}.nav{justify-content:center}}</style></head><body>'
echo '<nav class=nav><a href=# class=active>Home</a>'
for f in "${FILES[@]}"; do
  n=$(basename "$f")
  echo "<a href=\"#$n\">$(echo "$n"|sed 's/.html//;s/_/ /g')</a>"
done
echo '</nav><main class=content>'
for f in "${FILES[@]}"; do
  n=$(basename "$f")
  body=$(sed -n '/<body[^>]*>/,/<\/body>/p' "$f"|sed '1s/.*<body[^>]*>//;$s/<\/body>.*//')
  echo "<div class=card id=\"$n\">$body</div>"
done
echo '</main>'
echo '<script>document.querySelectorAll(".nav a").forEach(a=>{a.onclick=function(e){e.preventDefault();document.querySelectorAll(".nav a").forEach(x=>x.classList.remove("active"));this.classList.add("active");let t=document.querySelector(this.getAttribute("href"));if(t)t.scrollIntoView({behavior:"smooth",block:"start"})}})</script>'
echo '</body></html>'
} > "$WDIR/weaved.html"
echo "Weaved ${#FILES[@]} files -> $WDIR/weaved.html"
