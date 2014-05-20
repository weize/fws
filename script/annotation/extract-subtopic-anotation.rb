#!/usr/bin/ruby
$:.unshift File.expand_path('.')
require 'json'
require 'annotation-backend.rb'

fileName = "../exp/annotation/from-db/feedback-annotation.json"
db = AnnotationDB.new

feedbacks = db.getAllFeedbacks()

file = File.open(fileName, "w")
feedbacks.each do |feedback|
 #   puts annotation.to_json #if db.annotationComplete?(annotation)
    file.write(feedback.to_json)
    file.write("\n");
end

file.close()
puts "Written in " + fileName
