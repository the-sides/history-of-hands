const rock = `🪨`
function Card() {
    return <div className="w-[210px] h-[300px] bg-[#2D163F]"><p className="text-[256px]">{rock}</p></div>
}
export default function CardSlider() {
    return <Card/>
}