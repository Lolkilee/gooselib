<script lang="ts">
    import { triggerRemoveAppModal } from '$lib/modals.js';
    import { formatBytes } from '$lib/util';
    import { getModalStore, getToastStore } from '@skeletonlabs/skeleton';
    import { invoke } from '@tauri-apps/api';

    export let data;

    const modalStore = getModalStore();

    let inst = false;

    $: installed = inst;

    async function checkIfInstalled() {
        await invoke('check_dir_exists', {
            dir: data.installPath,
        }).then((val) => {
            inst = !!val;
        });
    }

    function remove() {
        modalStore.trigger(triggerRemoveAppModal(data.installPath));
    }

    // Called every second
    async function update() {
        while (true) {
            await checkIfInstalled();
            await new Promise((resolve) => setTimeout(resolve, 1000));
        }
    }

    checkIfInstalled();
    update();
</script>

<div class="flex flex-col justify-between">
    <div>
        <h1 class="h1 mb-10">{data.slug}</h1>
        {#if installed}
            <button class="btn variant-filled-primary mb-10">Run</button>
            <button
                class="btn variant-filled-error mb-10 ml-10"
                on:click={remove}>Delete</button
            >
        {:else}
            <button class="btn variant-filled-primary mb-10">Install</button>
        {/if}
        <hr class="!border-t-4 my-2" />
    </div>
    <div class="p-4">
        <h4 class="h4 mb-4">App info</h4>
        <p class="text-slate-400">
            latest version: {data.metaData?.latestVersion}
        </p>
        <p class="text-slate-400">Install folder: {data.installPath}</p>
        <p class="text-slate-400">
            App size: {formatBytes(data.metaData?.bytesCount, 2)}
        </p>
    </div>
</div>
