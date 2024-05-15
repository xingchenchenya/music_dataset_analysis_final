from flask import render_template,Flask

app = Flask(__name__)

@app.route('/view')
def req_file():
    return render_template("view.html")
         
if __name__ == '__main__':
    app.DEBUG=True 
    app.run(host='0.0.0.0', debug=True, port=5000)

