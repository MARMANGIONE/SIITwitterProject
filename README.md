## TAS: tweet acquisition system

This version of the Mehdi Sadri code doesn't have the dimple library and includes natural language processing inside.

=================

Twitter provides a public streaming API that is strictly limited, making it difficult to simultaneously achieve good coverage and relevance when monitoring tweets for a specific topic of interest. In TAS, we address the tweet acquisition
challenge to enhance monitoring of tweets based on the client/application needs in an online adaptive manner such that the quality and quantity of the results improves over time. We propose a Tweet Acquisition System (TAS), that iteratively selects phrases to track based on an exploreexploit strategy and dynamically adapts the representation of topic of interest. Our experimental studies show that TAS significacantly improves recall of relevant tweets and the performance improves when the topics are more specific.

## Prerequisites: MongoDB
In the config.txt file on line 12 and 13 there is the database host. In my case the database is local. For the project to work, however it is run, it is necessary to start MongoDB. This operation is performed through two terminal windows: in the first typing mongod we will start the server process, in the second, instead, typing mongo we will start the shell, which will allow us to view the content inside the database.

=================

## Install Java Application 
Import the project as a Maven project.
Once the artifact has been installed, before the execution of the project, it will be necessary to start the local database from the terminal. Once the database is started, the Java Application can be executed in two ways:
1. <b>InterestStorageManager class </b> allows you to establish a connection with the Mongo database and allows you to add, view and remove Interest. The interest is characterized by an id, a name, a client-id, a client-name and a list of phrases that are those that will allow us to carry out the search. 
2. <b>MainTA class </b> allows based on the interest chosen to begin the acquisition of the Tweets. When it is executed, several threads are started: one for starting the Twitter stream, the second for the acquisition based on the filters (ie the sentences previously entered in the Interest). While the application is listening you can perform multiple operations by typing simple keyboard commands:
<b> smc - Storage Manager Queue Size </br>
tc - Total Tweet Count </br>
wtc - Window Tweet Count </br>
tce - Total English Tweet Count </br>
tcr - Total Relevant Tweet Count </br>
wtcr - Window Relevant Tweet Count </br>
tcd - Total Delta Tweet Count </br>
tcp - Total Processing Buffer Tweet Count </br>
</b>
