import type { PageLoad } from './$types';

export const load: PageLoad = ({ params }) => {
    let loadedApp: AppDefinition;
    const appName = params.slug;
    const jString = sessionStorage.getItem("app-data");
    if (jString != null) {
        const lib: Library = JSON.parse(jString);
        loadedApp = lib.apps[0];

        for (let i = 0; i < lib.apps.length; i++) {
            const tmp = lib.apps[i];
            if (tmp.name == appName) {
                loadedApp = tmp;
            }
        }
    } else {
        loadedApp = new AppDefinition("Internal error", []);
    }

    return {
        app: loadedApp,
        selectedVersion: loadedApp.versions[0]
    };
  };