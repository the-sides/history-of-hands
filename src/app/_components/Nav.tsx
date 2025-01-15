import Link from "next/link";
import { auth } from "~/server/auth";

export default async function Nav() {
    const session = await auth();
    return <nav className="flex w-full justify-between">
        <div className="flex-1" />
        <h1 className="flex-1 text-center text-[7rem]"><Link href={'/'}>History of Hands</Link></h1>
        <div
            className="text-6xl text-right flex-1"
        >
            <a className="block p-12" href={session ? "/api/auth/signout" : "/api/auth/signin"}>{session ? "Sign Out" : "Sign In"}</a>
        </div>
    </nav>
}