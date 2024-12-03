/// <reference lib="deno.unstable" />
const kv = await Deno.openKv();

const createSlugName = (name: string) => name.toLowerCase().replaceAll(" ", "-");

export async function getList(slugName: string) {
    const list = await kv.get(["lists", slugName]);
    if(list.value === null) {
        createList(slugName)
    }
    return list;
}


export async function createList(name: string) {
    console.log("Creating list", name);
    const slugName = createSlugName(name);
    const key = ["lists", slugName];
    const value = { user: slugName, name, items: [] };
    await kv.set(key, value);
}