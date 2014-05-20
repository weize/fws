#!/usr/bin/ruby
usage = "
ruby addAolUserSessionsToMongo.rb <user file>

<user file> should be in the format of one JSON string per line with two fields:

    userID
    sessions

"

$:.unshift File.expand_path('.')
require 'annotation-backend.rb'
require 'json'


inputFile = "../exp/annotation/pool.json"
db = AnnotationDB.new()

IO.foreach(inputFile) do |line|
    begin
        info = JSON.parse(line.chomp)
        db.addAolSessions( info['userID'],info['qaspectQuery'], info['sessions'])
	puts "added " + info['userID']
    rescue Exception
        puts "Problem adding line: #{line}"
        exit
    end
end
