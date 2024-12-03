/// <reference lib="deno.unstable" />
const kv = await Deno.openKv();

const createSlugName = (name: string) => name.toLowerCase().replaceAll(" ", "-");

export async function getList(slugName: string) {
    const list = await kv.get(["lists", slugName]);
    if(list.value === null) {
        return null;
    }
    return list;
}

// Don't over do it for now

export async function createTournament(name: string, participants: string[]) {
    console.log("Creating tournament:", name, "for", participants);
    const slugName = createSlugName(name);
    const key = ["tournaments", slugName];
    const value = { users: slugName, name, items: [] };
    await kv.set(key, value);
}