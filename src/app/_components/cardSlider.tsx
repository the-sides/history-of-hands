'use client';

import type { User } from "@prisma/client";
import { useEffect, useRef, useState } from "react";
import type { typeOfHand } from "../models/game";


interface CardSliderProps {
    selected: typeOfHand | null,
    handleSelect: (type: typeOfHand | null) => void,
    user: User,
    isOwner: boolean
}

export default function CardSlider({selected, handleSelect, user, isOwner }: CardSliderProps) {
    const scrollElm = useRef<HTMLDivElement>(null);
    const [visible, setVisible] = useState<boolean>(false)

    useEffect(() => {
        if (scrollElm.current) {
            scrollElm.current.scrollLeft = 99999;
        }
        setVisible(true)
    }, [])

    function Card({ type }: { type: typeOfHand }) {
        return <button onClick={() => handleSelect(type)} className="flex-shrink-0 w-[100px] h-[150px]  md:w-[210px]  md:h-[300px] bg-[#2D163F] rounded-md shadow-xl"><p className="text-xl md:text-[56px]">{type}</p></button>
    }

    const types: typeOfHand[] = ['ROCK', 'PAPER', 'SCISSORS']
    return <div className={`w-[calc(50%-2px)] md:w-[calc(50%-20px)] flex relative items-center overflowd-hidden transition-opacity ${selected ? 'mt-32 flex-col' : 'h-[calc(((50vw*sqrt(3))/3)+330px)]'} ${visible ? 'opacity-100' : 'opacity-0'}`} style={{ scrollbarWidth: 'none', maskImage: 'linear-gradient(to right, transparent, black 10%, black 90%, transparent)' }}>
        <p className={` text-center max-w-[500px] text-2xl md:text-6xl h-fit z-10 ${selected ? '' : 'absolute top-16 sm:top-24 '} inset-x-0 mx-auto origin-center transition-transform ${selected === null && (isOwner ? 'rotate-[30deg]' : 'rotate-[-30deg]')}`}><span className={isOwner ? 'text-blue-400' : 'text-red-400'}>{user.name}</span> Threw:</p>
        {/* {selected === null ? */}
        {/* Needs better transition animations */}
        <div className={`flex items-center relative  ${selected && 'hidden opacity-0'} ${isOwner ? 'translate-x-[-20%]' : 'translate-x-[-20%]'}`}>
            <div ref={isOwner ? scrollElm : undefined} className={`w-[78vw] overflow-scroll flex-shrink-0 flex gap-2 ${isOwner ? 'rotate-[30deg]' : 'rotate-[-30deg]'}`} style={{ scrollbarWidth: 'none' }} >
                {[...types, ...types, ...types].map((type, ind) =>
                    <Card key={type + ind + String(isOwner)} type={type} />
                )}
            </div>
        </div>
        {/* : */}
        <div className={`flex flex-col items-center justify-center w-full transition-opacity ${selected === null && 'opacity-0'}`}>
            <p className="text-5xl md:text-9xl">{selected}</p>
            <button className="border px-2 mt-4 text-xl md:text-4xl" onClick={() => handleSelect(null)}>cancel</button>
        </div>
        {/* } */}
    </div>
}