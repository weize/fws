use strict;
use JSON;

# config file
my $configFile = "../exp/parameter/config.json";
my $in = openReader($configFile);
my $content = join("", <$in>);
close($in);
our $config = decode_json $content;

sub openReader {
	my $filename  = shift;
	open my $in, "<", $filename or die "cannot open $filename";
	return $in;
}

sub openWrite {
	my $filename  = shift;
	open my $out, ">", $filename or die "cannot open $filename";
	return $out;
}

1;
