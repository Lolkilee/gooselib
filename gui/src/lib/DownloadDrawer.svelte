<script lang="ts">
    import { ProgressBar } from '@skeletonlabs/skeleton';
    import type { DownloadInfo } from './download';
    import { downloadInfoStore } from './stores';
    import { formatBytes } from './util';

    let downloads: DownloadInfo[] = [];

    $: infos = downloads;

    downloadInfoStore.subscribe((val) => {
        downloads = val;
    });
</script>

<div class="p-8">
    <h1 class="h2 mb-10">Downloads</h1>
    {#each infos as info}
        <div class="flex card p-4 my-2 justify-between">
            <p class="pt-2">{info.appName}</p>
            <div class="flex py-2 w-3/4">
                {#if info.netwProgress != 1}
                    <p class="pr-10 w-1/4">{formatBytes(info.speed)}/s</p>
                {:else}
                    <p class="pr-10 w-1/4">Installing...</p>
                {/if}
                <div class="grid h-full w-full">
                    <ProgressBar
                        class="col-start-1 row-start-1 mb-1"
                        value={info.netwProgress * 100}
                        max={100}
                    />
                    <ProgressBar
                        meter="col-start-1 row-start-1 variant-filled-primary"
                        value={info.writeProgress * 100}
                        max={100}
                    />
                </div>
            </div>
        </div>
    {/each}
</div>
