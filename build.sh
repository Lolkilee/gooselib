#!/bin/bash
platforms=("x86_64-unknown-linux-gnu" "x86_64-pc-windows-msvc" "x86_64-apple-darwin" "aarch64-apple-darwin")
for plat in ${platforms[@]}; do
    deno compile -A -o ./build/gl-downloader-$plat --target $plat ./svr/gl-downloader.ts
done