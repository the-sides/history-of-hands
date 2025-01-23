// I'm not using this directly, I just wanted reference to the styles incase I want to use it later. 
export default function PoppedInput() {
    return <div className="relative w-fit after:content-[''] after:w-full after:h-full after:absolute after:inset-0 after:translate-x-8 after:translate-y-8 after:border-white after:border-[8px] after:-z-10">
        <input type="text" name="guest" id="guest" className="bg-[rgb(28,17,61)] border-[8px] border-white text-8xl px-7 py-6  " />
    </div>
}