namespace smithy.example

@aws.api#requiredActions(["iam:PassRole", "ec2:RunInstances"])
operation MyOperation()
