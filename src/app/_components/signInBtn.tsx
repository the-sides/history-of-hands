'use client';

import { signIn, signOut } from "next-auth/react";

export default function AuthButton({ asSignIn, className, children }: { asSignIn: boolean, className: string, children: any }) {
    return <button
        className={className}
        onClick={() => {
            asSignIn ?
                signIn('discord') :
                signOut()
        }}
    >
        {children}
    </button>
}