Put launcher icons here before building:

  32x32.png
  128x128.png
  icon.ico   (Windows)
  icon.png   (Linux)

Easiest way — generate them all from one square PNG (>=512x512):

  cargo tauri icon path/to/logo.png

That command writes the full icon set into this folder automatically.
