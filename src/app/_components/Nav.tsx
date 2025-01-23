import Link from "next/link";
import { auth } from "~/server/auth";
import GoBack from "./goBack";

export default async function Nav() {
    const session = await auth();
    return <nav className="flex w-full justify-between">
        <div className="flex-1"><GoBack/></div>
        <h1 className="flex-1 text-center py-6 text-[7rem] leading-[5rem]"><Link href={'/'}>History of Hands</Link></h1>
        <div
            className="text-5xl text-right flex-1"
        >
            {
                session?.user && 
                <a className="block p-12" href="/api/auth/signout">Sign Out</a>
                
            }
        </div>
    </nav>
}