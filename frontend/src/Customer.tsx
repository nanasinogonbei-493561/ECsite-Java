import { useState } from "react";

type Age = 
| { age: '20歳以上ですか？' }
| { age: '20歳未満ですか？' };

const [age , setAge] = useState<Age>({ age: '20歳以上ですか？' });

export default function CustomerAge() {
    
}