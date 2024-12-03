import { useSignal } from "@preact/signals";
import { Button } from "../components/Button.tsx";

interface NameModalProps {
  existingName: string;
}

export default function NameModal(props: NameModalProps) {
  const input = useSignal("");
  if(props.existingName) return null;

  return (
    <div class="absolute inset-0 h-full w-full overflow-hidden">
      {/* greyed backdrop */}
      <div className="absolute inset-0 m-auto h-fit rounded-md p-6 pb-8 w-fit z-10 max-w-[600px] bg-slate-200 text-black reveal delay-500">
        <h3 class={"text-2xl font-bold w-full px-4 text-center"}>Welcome to QuickWish</h3>
        <p class={"text-xl w-full px-4 text-center"}>Enter your name to get started.</p>
        <p class={"mt-4"}>
          This name will be shared with others and is used to ensure your
          identity against the wishlist owner.
        </p>
        <p>
          The wishlist owner will not be able to see activity made on the
          wistlist. But you will be able to see the gifts others have already
          purchased.
        </p>
        <p>
          If you're used this site on other devices, make sure you're using the
          same name.
        </p>
        <div className="flex justify-center mt-4 gap-3">
          <div className="">
            <label htmlFor="name" class="block text-sm font-medium text-gray-700">
              Name
            </label>
            <input
              class="border-2 border-gray-500 rounded p-2"
              placeholder="Enter your name"
              onInput={(e) => {
                input.value = (e.target as HTMLInputElement).value as string;
              }}
            />
          </div>
          <Button
          class={'h-11 self-end'}
            onClick={async () => {
              try {
                console.log("Saving name:", input.value);
                const response = await fetch("/api/save-name", {
                  method: "POST",
                  headers: {
                    "Content-Type": "application/json",
                  },
                  body: JSON.stringify({ name: input.value }),
                });

                if (response.ok) {
                  // Assuming the server responds with the saved name, or some confirmation
                  const result = await response.text();
                  console.log("Saved name:", result);
                } else {
                  // Handle errors if needed
                  console.error("Failed to save the name");
                }
              } catch (error) {
                console.error("An error occurred:", error);
              }
            }}
          >
            Save
          </Button>
        </div>
      </div>
      <div className="absolute inset-0 h-full w-full bg-black bg-opacity-60 reveal">
      </div>
    </div>
  );
}

// <div class="flex gap-8 py-6">
//   {props.existingName.value
//     ? (
//       <div class="flex-1">
//         <h1 class="text-2xl font-bold">
//           {props.existingName.value}
//         </h1>
//         <p class="text-gray-500">
//           This name is already in use. Please choose another.
//         </p>
//       </div>
//     )
//     : (
//       <div class="flex-1">
//         <h1 class="text-2xl font-bold">
//           What is your name?
//         </h1>
//         <p class="text-gray-500">
//           This name will be shared with others. Please choose carefully.
//         </p>
//       </div>
//     )}
//   <input
//     class="border-2 border-gray-500 rounded p-2"
//     placeholder="Enter your name"
//     onInput={(e) => {
//       input.value = (e.target as HTMLInputElement).value as string;
//     }}
//   />
//   <Button
//     onClick={() => {
//       props.existingName.value = "";
//     }}
//   >
//     Cancel
//   </Button>
//   <Button
//     onClick={async () => {
//       try {
//         console.log("Saving name:", input.value);
//         const response = await fetch("/api/save-name", {
//           method: "POST",
//           headers: {
//             "Content-Type": "application/json",
//           },
//           body: JSON.stringify({ name: input.value }),
//         });

//         if (response.ok) {
//           // Assuming the server responds with the saved name, or some confirmation
//           const result = await response.text();
//           console.log("Saved name:", result);
//         } else {
//           // Handle errors if needed
//           console.error("Failed to save the name");
//         }
//       } catch (error) {
//         console.error("An error occurred:", error);
//       }
//     }}
//   >
//     Save
//   </Button>
// </div>
