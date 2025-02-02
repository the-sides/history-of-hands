import Link from "next/link";
import { auth } from "~/server/auth";
import GoBack from "./goBack";
import AuthButton from "./signInBtn";

export default async function Nav() {
    const session = await auth();
    return <nav className="flex w-full justify-between">
        <div className="max-md:hidden fledx-1 text-3xl xl:text-5xl"><GoBack/></div>
        <h1 className="md:flex-1 text-center py-6 text-[5rem] lg:text-[7rem] leading-[5rem]"><Link href={'/'}>History of Hands</Link></h1>
        <div
            className="text-3xl xl:text-5xl flex justify-end items-start fdlex-1"
        >
            {
                session?.user && 
                <AuthButton className="block p-12" asSignIn={false}>Sign Out</AuthButton>
            }
        </div>
    </nav>
}