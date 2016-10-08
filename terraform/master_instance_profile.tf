resource "aws_iam_instance_profile" "master" {
  name = "master-${var.stack_name}"
  path = "/"

  roles = ["${aws_iam_role.master.name}"]
}
