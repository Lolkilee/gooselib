import { getAppMetaData } from "$lib/metadata"

//@ts-ignore
export const load = ({ params }) => {
    const metaData = getAppMetaData(params.slug);
    let installPath = './' + params.slug;

    const parentPath = localStorage.getItem('installFolder');
    if (parentPath)
        installPath = parentPath + '/' + params.slug;

    installPath.replace('\\', '/');

    return {
        slug: params.slug,
        metaData: metaData,
        installPath: installPath
    }
}
