<script lang="ts">
    import { page } from "$app/stores";

    let app: AppDefinition;

    function loadPageData() {
        const appName = $page.params.slug;
        const lib: Library = JSON.parse(sessionStorage.getItem("app-data"));
        app = lib.apps[0];

        for (let i = 0; i < lib.apps.length; i++) {
            const tmp = lib.apps[0];
            if (tmp.name == appName) app = tmp;
        }
    }

    loadPageData();
</script>

<h1 class="h1">{app.name}</h1>
