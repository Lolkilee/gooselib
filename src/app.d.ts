// See https://kit.svelte.dev/docs/types#app
// for information about these interfaces
declare global {
	namespace App {
		// interface Error {}
		// interface Locals {}
		// interface PageData {}
		// interface Platform {}
        
    }
    
    class AppDefinition {
        name: string;
        versions: string[];

        constructor(name: string, versions: string[]) {
            this.name = name;
            this.versions = versions;
        }
    }

    interface Library {
        apps: AppDefinition[];
    }
}

export {};
