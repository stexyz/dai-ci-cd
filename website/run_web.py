import tornado.ioloop
import tornado.web
import boto3
import io
import pandas as pd

def load_csv_from_s3():
    # get a handle on s3
    s3 = boto3.resource(u's3',
                  endpoint_url='http://localhost:9000',
                  aws_access_key_id='accesskey',
                  aws_secret_access_key='secretkey',
                  region_name='us-east-1')
    bucket = s3.Bucket(u'data-bucket')
    obj = bucket.Object(key=u'creditcard_train_cat.csv')
    response = obj.get()
    df = pd.read_csv(io.BytesIO(response['Body'].read()))
    return df

class MainHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('<h1>H2O.ai Scoring Example</h1><br /><table>')
        df = load_csv_from_s3()
        for index, row in df.head().iterrows():
            self.write('<tr>')
            for index, item in row.items():
                self.write('<td>')
                self.write(item)
                self.write('</td>')
            self.write('</tr>')
        self.write('</table>')
        
if __name__ == "__main__":
    app = tornado.web.Application([(r"/", MainHandler),])
    app.listen(9999)
    tornado.ioloop.IOLoop.current().start()