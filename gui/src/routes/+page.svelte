<script lang="ts">
    import { goto } from '$app/navigation';
    import type { MetaData, MetaLibrary } from '$lib/metadata';
    import { metaLibStore, selectedMetaStore } from '$lib/stores';
    import {
        Autocomplete,
        type AutocompleteOption,
    } from '@skeletonlabs/skeleton';

    let metaLib: MetaLibrary;
    let input = '';
    let autoCompleteList: AutocompleteOption<MetaData>[] = [];

    metaLibStore.subscribe((val) => {
        metaLib = val;
        generateAutoComplete();
    });

    function generateAutoComplete() {
        if (metaLib.length != autoCompleteList.length) {
            autoCompleteList = [];
            metaLib.forEach((meta) => {
                const opt: AutocompleteOption<MetaData> = {
                    label: meta.name,
                    value: meta,
                };
                autoCompleteList.push(opt);
            });
        }
    }

    function navigateToPage(appName: string) {
        const route = `/app/${appName}`;
        goto(route);
    }

    function onSelection(
        event: CustomEvent<AutocompleteOption<MetaData>>
    ): void {
        input = event.detail.label;
        selectedMetaStore.set(event.detail.value);
        navigateToPage(input);
    }

    generateAutoComplete();
</script>

<div class="flex flex-col w-full justify-between items-center">
    <h1 class="h1 my-10">Available apps</h1>
    <div class="mb-30 w-3/4">
        <input
            class="input my-4"
            type="search"
            name="demo"
            bind:value={input}
            placeholder="Search apps..."
        />

        <div class="card w-full p-4 max-h-64 overflow-y-auto" tabindex="-1">
            <Autocomplete
                bind:input
                options={autoCompleteList}
                on:selection={onSelection}
            />
        </div>
    </div>
</div>
