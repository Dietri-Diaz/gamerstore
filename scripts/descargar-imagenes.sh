#!/usr/bin/env bash
set -e
DIR="uploads/productos"
mkdir -p "$DIR"

# slug|url  (fotos libres Unsplash; ?w=800&q=80)
items=(
  "rtx-4060|https://images.unsplash.com/photo-1591488320449-011701bb6704?w=800&q=80"
  "rtx-4070|https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800&q=80"
  "rtx-4080|https://images.unsplash.com/photo-1555618254-84e5f7d1e0f2?w=800&q=80"
  "rx-7800xt|https://images.unsplash.com/photo-1591238372338-22d30c883f5b?w=800&q=80"
  "ryzen-5-5600|https://images.unsplash.com/photo-1555617981-dac3880eac6e?w=800&q=80"
  "ryzen-7-7800x3d|https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=800&q=80"
  "intel-i5-13600k|https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80"
  "intel-i7-13700k|https://images.unsplash.com/photo-1591405351990-4726e331f141?w=800&q=80"
  "mb-b550|https://images.unsplash.com/photo-1518774147153-2a3f1f0f0a3f?w=800&q=80"
  "mb-b650|https://images.unsplash.com/photo-1600348759986-9c1b8f4c0a3a?w=800&q=80"
  "mb-z790|https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&q=80"
  "ram-vengeance-16|https://images.unsplash.com/photo-1541029071515-84cc54f84dc5?w=800&q=80"
  "ram-vengeance-32|https://images.unsplash.com/photo-1562976540-1502c2145186?w=800&q=80"
  "ssd-980-1tb|https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=800&q=80"
  "ssd-sn850x-2tb|https://images.unsplash.com/photo-1531492746076-161ca9bcad58?w=800&q=80"
  "psu-rm750|https://images.unsplash.com/photo-1587134160368-5d9c2f5c9e9a?w=800&q=80"
  "monitor-odyssey-g7|https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80"
  "monitor-lg-ultragear|https://images.unsplash.com/photo-1616711906333-23cf8b918a76?w=800&q=80"
  "teclado-blackwidow|https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=800&q=80"
  "mouse-gpro-superlight|https://images.unsplash.com/photo-1527814050087-3793815479db?w=800&q=80"
  "headset-cloud-iii|https://images.unsplash.com/photo-1599669454699-248893623440?w=800&q=80"
  "silla-titan-evo|https://images.unsplash.com/photo-1598550476439-6847785fcea6?w=800&q=80"
  "silla-cougar|https://images.unsplash.com/photo-1610395219791-21b0353e43c1?w=800&q=80"
  "ps5-slim|https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=800&q=80"
  "xbox-series-x|https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=800&q=80"
  "switch-oled|https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?w=800&q=80"
  "webcam-brio|https://images.unsplash.com/photo-1596742578443-7682ef5251cd?w=800&q=80"
  "cooler-aio|https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=800&q=80"
)

for it in "${items[@]}"; do
  slug="${it%%|*}"; url="${it#*|}"
  echo "-> $slug"
  curl -fsSL "$url" -o "$DIR/$slug.jpg" || echo "   (falló $slug, se reintenta manual)"
done
echo "Listo: $(ls -1 "$DIR" | wc -l) imágenes en $DIR"
