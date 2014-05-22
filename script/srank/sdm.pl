use strict;
require 'config.pl';
our $fws;
our $trec_eval;
our $config;  

my $rankFile = $config->{"rankedListFile"};
my $srankFile = $config->{"sdmSrank"};
my $queryJsonFile = $config->{"queryJsonFile"};
my $qrelSubtopic = $config->{"qrelSubtopic"};
my $evalFile = $config->{"sdmSeval"};
my $evalTrecFile = $config->{"sdmSevalTrec"};

my $cmd;

# copy topic rank for subtopics
$cmd = "fws rank-to-srank --queryJsonFile=$queryJsonFile --topicRankFile=$rankFile --output=$srankFile";
call($cmd);

# eval
$cmd = "fws eval-ranking --qrel=$qrelSubtopic --rank=$srankFile --trecEval=$trec_eval --output=$evalFile --trecOutput=$evalTrecFile";
call($cmd);


