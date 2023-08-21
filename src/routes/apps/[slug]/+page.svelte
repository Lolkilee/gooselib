<script lang="ts">
    import { page } from "$app/stores";
    // @ts-ignore
    import Flex from "svelte-flex";
    import { ProgressBar } from "@skeletonlabs/skeleton";
    import { invoke } from "@tauri-apps/api/tauri";
    import { emit, listen } from "@tauri-apps/api/event";
    import { onDestroy } from "svelte";

    let app: AppDefinition;
    let downloadProgress: any;
    let selectedVersion = "";

    function getProgress() {
        invoke("get_download_progress").then(
            (progress) => (downloadProgress = progress)
        );
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

    function installApp() {
        const address = localStorage.getItem("saved-address");
        const password = localStorage.getItem("server-password");
        const installFolder = localStorage.getItem("install-folder");

        console.log(address);
        console.log(password);
        console.log(installFolder);

        if (address != null && password != null && installFolder != null) {
            const url =
                "http://" +
                address +
                ":8765/" +
                app.name +
                "/" +
                selectedVersion +
                ".app";
            const path =
                installFolder + "/" + app.name + "." + selectedVersion + ".app";
            console.log(url);
            console.log(path);
            invoke("download_app", { url: url, path: path });
        }
    }

    const updateInterval = setInterval(() => {
        getProgress();
    }, 100);
    onDestroy(() => clearInterval(updateInterval));

    loadPageData();
</script>

<h1 class="h1 mb-12">{app.name}</h1>

<div class="my-2">
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
    <Flex justify="between">
        <button
            on:click={installApp}
            type="button"
            class="relative inset-y-0 left-0 btn variant-filled-primary mt-16"
        >
            Install app
        </button>
        <div class="w-2/3">
            <ProgressBar
                class="w-2/3"
                label="Progress Bar"
                value={downloadProgress}
                max={100}
            />
        </div>
    </Flex>
</div>
