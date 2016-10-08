resource "aws_iam_access_key" "host_keys" {
  user = "${aws_iam_user.dcos.name}"
}

resource "aws_iam_user" "dcos" {
  name = "dcos-${var.stack_name}"
}

resource "aws_iam_user_policy" "dcos" {
    name = "dcos"
    user = "${aws_iam_user.dcos.name}"
    policy = <<EOF
{
  "Statement": [
    {
      "Resource": [
        "arn:aws:s3:::${aws_s3_bucket.exhibitor.id}/*",
        "arn:aws:s3:::${aws_s3_bucket.exhibitor.id}"
      ],
      "Action": [
        "s3:AbortMultipartUpload",
        "s3:DeleteObject",
        "s3:GetBucketAcl",
        "s3:GetBucketPolicy",
        "s3:GetObject",
        "s3:GetObjectAcl",
        "s3:ListBucket",
        "s3:ListBucketMultipartUploads",
        "s3:ListMultipartUploadParts",
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Effect": "Allow"
    },
    {
      "Resource": "*",
      "Action": [
        "ec2:DescribeKeyPairs",
        "ec2:DescribeSubnets",
        "autoscaling:DescribeLaunchConfigurations",
        "autoscaling:UpdateAutoScalingGroup",
        "autoscaling:DescribeAutoScalingGroups",
        "autoscaling:DescribeScalingActivities",
        "elasticloadbalancing:DescribeLoadBalancers"
      ],
      "Effect": "Allow"
    }
  ],
  "Version": "2012-10-17"
}
EOF
}
