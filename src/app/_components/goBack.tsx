'use client';
import { usePathname } from "next/navigation";

export default function GoBack() {
    const path = usePathname();
    if(path !== '/') {
        return <a className="text-5xl block p-12" href="/">Go Back</a>
    }
    return null;
}