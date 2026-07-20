# -*- coding: utf-8 -*-
"""
Convierte la documentación en Markdown (DOC-*.md) a PDF con un diseño limpio.

Cómo funciona:
  1. Lee el .md y lo convierte a HTML con la librería `markdown`
     (con soporte de tablas y bloques de código).
  2. Lo envuelve en una plantilla HTML con CSS pensado para impresión
     (portada, índice, saltos de página por sección, tablas legibles).
  3. Llama a Chrome en modo headless para imprimir ese HTML a PDF.

Uso:  python generar_pdf_docs.py
Requisitos:  pip install markdown   ·   Google Chrome instalado
"""
import re
import subprocess
import sys
from pathlib import Path

import markdown

DOCS = Path(__file__).resolve().parent

# Cada documento con su portada (título, subtítulo y color de acento)
DOCUMENTOS = [
    ("DOC-GENERAL.md",  "Documentación general",   "Arquitectura, módulos y cómo se conecta todo",        "#4f46e5", "#7c3aed"),
    ("DOC-BACKEND.md",  "Documentación · Backend", "Spring Boot · qué hace cada archivo y qué modificar", "#0f766e", "#14b8a6"),
    ("DOC-FRONTEND.md", "Documentación · Frontend", "React + Vite · qué hace cada archivo y qué modificar", "#b45309", "#f59e0b"),
]

CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

PLANTILLA = """<!DOCTYPE html>
<html lang="es"><head><meta charset="UTF-8"><title>{titulo}</title><style>
  @page {{ size: A4; margin: 16mm 14mm; }}
  * {{ box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }}
  body {{ font-family: "Segoe UI",-apple-system,Roboto,Helvetica,Arial,sans-serif;
         color:#1e293b; font-size:10pt; line-height:1.6; margin:0; }}

  /* ---- Portada ---- */
  .portada {{ page-break-after: always; padding-top: 40mm; }}
  .hero {{ background:linear-gradient(135deg,{c1} 0%,{c2} 100%); color:#fff;
          border-radius:18px; padding:38px 32px; }}
  .hero .tag {{ display:inline-block; background:rgba(255,255,255,.22); padding:5px 13px;
               border-radius:999px; font-size:9pt; font-weight:700; }}
  .hero h1 {{ font-size:30pt; margin:14px 0 6px; line-height:1.1; }}
  .hero .sub {{ font-size:12.5pt; font-weight:600; opacity:.95; }}
  .portada .pie {{ margin-top:26px; font-size:9.5pt; color:#64748b; }}
  .portada .pie b {{ color:#334155; }}

  /* ---- Índice ---- */
  .toc {{ page-break-after: always; }}
  .toc h2 {{ border:0; margin:0 0 10px; padding:0; font-size:16pt; color:{c1}; }}
  .toc ul {{ list-style:none; padding-left:0; }}
  .toc > ul > li {{ margin:5px 0; font-weight:600; }}
  .toc ul ul {{ padding-left:16px; font-weight:400; font-size:9.2pt; color:#475569; }}
  .toc a {{ color:#334155; text-decoration:none; }}

  /* ---- Contenido ---- */
  h1 {{ font-size:19pt; color:{c1}; margin:0 0 12px; }}
  h2 {{ font-size:15pt; color:#fff; background:{c1}; padding:9px 14px; border-radius:8px;
       margin:0 0 14px; page-break-before: always; page-break-after: avoid; }}
  h2:first-of-type {{ page-break-before: avoid; }}
  h3 {{ font-size:12pt; color:{c1}; margin:18px 0 7px; padding-bottom:4px;
       border-bottom:2px solid #e2e8f0; page-break-after: avoid; }}
  h4 {{ font-size:10.5pt; color:#334155; margin:14px 0 5px; page-break-after: avoid; }}
  p {{ margin:0 0 8px; }}
  ul, ol {{ margin:0 0 10px; padding-left:20px; }}
  li {{ margin-bottom:3px; }}
  strong {{ color:#0f172a; }}
  hr {{ border:0; border-top:1px solid #e2e8f0; margin:18px 0; }}
  a {{ color:{c1}; }}

  /* Código: se parte para no salirse de la hoja */
  code {{ font-family:"Consolas","Cascadia Code",monospace; font-size:8.6pt;
         background:#eef2ff; color:#4338ca; padding:1px 5px; border-radius:4px;
         word-break:break-word; }}
  pre {{ background:#f8fafc; border:1px solid #e2e8f0; border-left:3px solid {c1};
        border-radius:7px; padding:10px 12px; overflow:hidden; page-break-inside:avoid; }}
  pre code {{ background:none; color:#334155; padding:0; font-size:8.3pt; line-height:1.5;
             white-space:pre-wrap; word-break:break-word; }}

  /* Tablas */
  table {{ width:100%; border-collapse:collapse; margin:10px 0 14px; font-size:8.8pt;
          page-break-inside:avoid; }}
  th {{ background:{c1}; color:#fff; text-align:left; padding:7px 9px; font-weight:700; }}
  td {{ padding:6px 9px; border-bottom:1px solid #e2e8f0; vertical-align:top;
       word-break:break-word; }}
  tr:nth-child(even) td {{ background:#f8fafc; }}

  blockquote {{ background:#fefce8; border-left:4px solid #f59e0b; margin:10px 0;
               padding:9px 14px; border-radius:0 7px 7px 0; color:#854d0e; font-size:9.4pt; }}
  blockquote p {{ margin:0; }}
</style></head><body>

<section class="portada">
  <div class="hero">
    <span class="tag">🎮 GAMERSTORE · DOCUMENTACIÓN TÉCNICA</span>
    <h1>{titulo}</h1>
    <div class="sub">{subtitulo}</div>
  </div>
  <div class="pie">
    Sistema de venta de tecnología · <b>Spring Boot</b> (API REST) + <b>React</b> (Vite) + <b>MySQL</b><br/>
    Los apartados marcados con <b>✏️</b> indican qué se puede modificar; los de <b>⚠️</b>, puntos delicados.
  </div>
</section>

{toc}
{contenido}
</body></html>
"""


def convertir(md_file: str, titulo: str, subtitulo: str, c1: str, c2: str) -> None:
    ruta = DOCS / md_file
    texto = ruta.read_text(encoding="utf-8")

    # El primer "# Título" del markdown ya lo muestra la portada: lo quitamos del cuerpo.
    texto = re.sub(r"^#\s+.*\n", "", texto, count=1)

    md = markdown.Markdown(extensions=["tables", "fenced_code", "toc", "sane_lists"])
    contenido = md.convert(texto)
    toc = f'<section class="toc"><h2>Contenido</h2>{md.toc}</section>'

    html = PLANTILLA.format(titulo=titulo, subtitulo=subtitulo, c1=c1, c2=c2,
                            toc=toc, contenido=contenido)

    html_tmp = DOCS / (ruta.stem + ".tmp.html")
    html_tmp.write_text(html, encoding="utf-8")

    pdf = DOCS / (ruta.stem + ".pdf")
    subprocess.run([CHROME, "--headless", "--disable-gpu", "--no-sandbox",
                    "--no-pdf-header-footer", f"--print-to-pdf={pdf}",
                    html_tmp.as_uri()], capture_output=True, timeout=180)
    html_tmp.unlink(missing_ok=True)

    if pdf.exists():
        print(f"  OK  {pdf.name}  ({pdf.stat().st_size // 1024} KB)")
    else:
        print(f"  ERROR: no se generó {pdf.name}")


if __name__ == "__main__":
    if not Path(CHROME).exists():
        sys.exit("No se encontró Google Chrome en: " + CHROME)
    print("Generando PDFs de la documentación...")
    for args in DOCUMENTOS:
        convertir(*args)
    print("Listo.")
