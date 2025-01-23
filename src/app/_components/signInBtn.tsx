'use client';

import { signIn, signOut } from "next-auth/react";

export default function AuthButton({ asSignIn, className, children }: { asSignIn: boolean, className: string, children: React.ReactNode }) {
    const handleClick = () => {
        return asSignIn ?
            signIn('discord').catch(console.error) :
            signOut().catch(console.error)
    }
    return <button
        className={className}
        onClick={handleClick}
    >
        {children}
    </button>
}