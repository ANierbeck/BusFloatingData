resource "aws_iam_instance_profile" "master" {
  name = "master"
  path = "/"
  roles = ["${aws_iam_role.master.name}"]
}
