$version: "1.0"

namespace smithy.example

use aws.cloudformation#cfnMutability

structure FooStructure {
    @cfnMutability("undefined")
    member: String
}
