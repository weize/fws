use strict;

my @files = <figure/*.eps>;

for my $file(@files) {
	system("convert $file $file.png");
	print "$file.png\n";
}
