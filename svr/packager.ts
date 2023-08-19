// Packager script for folder to .app conversion
// Input ./packager [input folder] [output file]

import { tar } from "https://deno.land/x/compress@v0.4.4/mod.ts";
import { lock } from "https://cdn.jsdelivr.net/gh/hexagon/lock@0.9.9/mod.ts";

const tmpFile = "./tmp.tar";

export async function pack(pass: string, input: string, output: string) {
    try {
        await tar.compress(input, tmpFile);
        await lock(tmpFile, false, false, true, false, pass);
        await Deno.rename(tmpFile + ".lock", output);
    } catch (err) {
        console.log(err);
    }
}

if (Deno.args[0] == "help" || Deno.args.length != 3) {
    console.log("Command format: [password] [input folder] [output file] \n Example: ./packager ./SomeFolder SomeApp.app")
}
else {
    const pass = Deno.args[0];
    const input = Deno.args[1];
    const output = Deno.args[2];

    pack(pass, input, output);
}