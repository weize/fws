#!/usr/bin/ruby
usage = "
ruby addQueryFullTopicsToMongo.rb <user file>

<user file> should be in the format of one JSON string per line with two fields:

    userID
    sessions

"

$:.unshift File.expand_path('.')
require 'annotation-backend.rb'
require 'json'


inputFile = "../data/query/query-full.json"; 
db = AnnotationDB.new()

IO.foreach(inputFile) do |line|
    begin
        info = JSON.parse(line.chomp)
        db.addFullTopic( info['number'],info['query'], info['subtopics'])
	puts "added " + info['number']
    rescue Exception
        puts "Problem adding line: #{line}"
        exit
    end
end
