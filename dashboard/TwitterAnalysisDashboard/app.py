from flask import Flask, jsonify, request
from flask import render_template
import ast
import json
app = Flask(__name__)

tweet_counters = {'total': 0, 'negative': 0, 'neutral': 0, 'positive': 0}
most_used_hashtags = {'labels': [], 'negative': [], 'neutral': [], 'positive': []}
most_active_users = {'labels': [], 'negative': [], 'neutral': [], 'positive': []}
most_mentioned_users = {'labels': [], 'negative': [], 'neutral': [], 'positive': []}


@app.route("/")
def get_dashboard_page():
	global most_used_hashtags, most_active_users, most_mentioned_users

	return render_template(
		'dashboard.html',
		tweet_counters=tweet_counters,
		most_used_hashtags=most_used_hashtags,
		most_active_users=most_active_users,
		most_mentioned_users=most_mentioned_users)


@app.route('/refresh_tweet_counters')
def refresh_tweet_counters_data():
	global tweet_counters
	return jsonify(
		sTotal=tweet_counters['total'],
		sNegative=tweet_counters['negative'],
		sNeutral=tweet_counters['neutral'],
		sPositive=tweet_counters['positive'])


@app.route('/refresh_most_used_hashtags')
def refresh_most_used_hashtags_data():
	global most_used_hashtags

	return jsonify(
		sLabel=most_used_hashtags['labels'],
		sNegative=most_used_hashtags['negative'],
		sNeutral=most_used_hashtags['neutral'],
		sPositive=most_used_hashtags['positive'])


@app.route('/refresh_most_active_users')
def refresh_most_active_users_data():
	global most_active_users

	return jsonify(
		sLabel=most_active_users['labels'],
		sNegative=most_active_users['negative'],
		sNeutral=most_active_users['neutral'],
		sPositive=most_active_users['positive'])


@app.route('/refresh_most_mentioned_users')
def refresh_most_mentioned_users_data():
	global most_mentioned_users

	return jsonify(
		sLabel=most_mentioned_users['labels'],
		sNegative=most_mentioned_users['negative'],
		sNeutral=most_mentioned_users['neutral'],
		sPositive=most_mentioned_users['positive'])


@app.route('/update_tweet_counters', methods=['POST'])
def update_tweet_counters_data():
	global tweet_counters
	if not request.form not in request.form:
		return "error", 400
	data = json.loads(request.form['JSON'])

	tweet_counters['total'] = data['total'].split(",")
	tweet_counters['negative'] = data['negative'].split(",")
	tweet_counters['neutral'] = data['neutral'].split(",")
	tweet_counters['positive'] = data['positive'].split(",")

	return "success", 201


@app.route('/update_most_used_hashtags', methods=['POST'])
def update_most_used_hashtags_data():
	global most_used_hashtags
	if not request.form not in request.form:
		return "error", 400
	data = json.loads(request.form['JSON'])

	most_used_hashtags['labels'] = data['label'].split(",")
	most_used_hashtags['negative'] = data['negative'].split(",")
	most_used_hashtags['neutral'] = data['neutral'].split(",")
	most_used_hashtags['positive'] = data['positive'].split(",")

	return "success", 201


@app.route('/update_most_active_users', methods=['POST'])
def update_most_active_users_data():
	global most_active_users
	if not request.form not in request.form:
		return "error", 400
	data = json.loads(request.form['JSON'])

	most_active_users['labels'] = data['label'].split(",")
	most_active_users['negative'] = data['negative'].split(",")
	most_active_users['neutral'] = data['neutral'].split(",")
	most_active_users['positive'] = data['positive'].split(",")

	return "success", 201


@app.route('/update_most_mentioned_users', methods=['POST'])
def update_most_mentioned_users_data():
	global most_mentioned_users
	if not request.form not in request.form:
		return "error", 400
	data = json.loads(request.form['JSON'])

	most_mentioned_users['labels'] = data['label'].split(",")
	most_mentioned_users['negative'] = data['negative'].split(",")
	most_mentioned_users['neutral'] = data['neutral'].split(",")
	most_mentioned_users['positive'] = data['positive'].split(",")

	return "success", 201


if __name__ == "__main__":
	app.run(host='localhost', port=5001)
