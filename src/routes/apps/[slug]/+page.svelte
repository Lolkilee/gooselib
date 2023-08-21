<script lang="ts">
    import { page } from "$app/stores";
    // @ts-ignore
    import Flex from "svelte-flex";
    import { ProgressRadial } from "@skeletonlabs/skeleton";
    import { invoke } from "@tauri-apps/api/tauri";
    import { onDestroy } from "svelte";
    import { Command } from "@tauri-apps/api/shell";

    let app: AppDefinition;
    let downloadProgress: number = 0;
    let status: string = "idle";
    let selectedVersion = "";

    interface ProgressUpdate {
        status: string;
        progress: number;
    }

    function loadPageData() {
        const appName = $page.params.slug;
        const jString = sessionStorage.getItem("app-data");
        if (jString != null) {
            const lib: Library = JSON.parse(jString);
            app = lib.apps[0];

            for (let i = 0; i < lib.apps.length; i++) {
                const tmp = lib.apps[0];
                if (tmp.name == appName) {
                    app = tmp;
                    selectedVersion = app.versions[0];
                }
            }
        }
    }

    async function installApp() {
        const address = localStorage.getItem("saved-address");
        const password = localStorage.getItem("server-password");
        const installFolder = localStorage.getItem("install-folder");

        if (address != null && password != null && installFolder != null) {
            const url =
                "http://" +
                address +
                ":8765/" +
                app.name +
                "/" +
                selectedVersion +
                ".app";
            console.log(url);

            //invoke("download_app", { url: url, path: path });
            const command = Command.sidecar("../svr/build/gl-downloader", [
                url,
                installFolder,
                password,
            ]);

            // Print error to debug console
            command.on("error", (error) =>
                console.error(`command error: "${error}"`)
            );
            command.stderr.on("data", (line) =>
                console.log(`command stderr: "${line}"`)
            );

            // On progress update
            command.stdout.on("data", (line) => {
                const update: ProgressUpdate = JSON.parse(line);
                downloadProgress = update.progress;
                status = update.status;
            });

            await command.execute();
        }
    }

    loadPageData();
</script>

<h1 class="h1 mb-12">{app.name}</h1>

<div class="my-2 pb-16">
    <Flex justify="between">
        <h5 class="h5">App version</h5>
        <select class="select w-2/3" bind:value={selectedVersion}>
            {#each app.versions as version}
                <option value={version}>{version}</option>
            {/each}
        </select>
    </Flex>
</div>

<div class="my-2">
    <Flex justify="evenly">
        <button
            on:click={installApp}
            type="button"
            class="relative inset-y-0 left-0 btn variant-filled-primary"
        >
            Install app
        </button>
        <div class="w-1/4">
            <ProgressRadial value={downloadProgress} width="w-24"
                >{downloadProgress}%</ProgressRadial
            >
        </div>
    </Flex>
</div>
