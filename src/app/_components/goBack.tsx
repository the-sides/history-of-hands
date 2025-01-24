'use client';
import Link from "next/link";
import { usePathname } from "next/navigation";

export default function GoBack() {
    const path = usePathname();
    if(path !== '/') {
        return <Link className="block p-12" href="/">Go Back</Link>
    }
    return null;
}