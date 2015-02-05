/*
 * Copyright (c) 2011-2015 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.io.net.http

import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import reactor.Environment
import reactor.io.codec.StandardCodecs
import reactor.io.net.NetStreams
import reactor.rx.Streams
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author Stephane Maldini
 */
class HttpSpec extends Specification {

	static final int port = 8080
	Environment env

	def setup() {
		env = Environment.initializeIfEmpty()
	}

	def "http responds to requests from clients"() {
		given: "a simple TcpServer"
			def stopLatch = new CountDownLatch(1)
			def server = NetStreams.httpServer {
				it.
						env(env).
						listen(port)
			}

		when: "the server is started"
			server.post('/test') { conn ->
				conn
						.decode(StandardCodecs.STRING_CODEC)
						.log('received')
						.consume()

				Streams
						.just("Hello World!")
						.map(StandardCodecs.STRING_CODEC)

			}

		then: "the server was started"
			server.start().awaitSuccess()

		when: "data is sent"
			def content = Request.Post("http://localhost:8080/test")
					.bodyString("hello", ContentType.TEXT_PLAIN)
					.execute()
					.returnContent()
					.asString()

		then: "data was recieved"
			content == "Hello World!"

		cleanup: "the server is stopped"
			server.shutdown().onSuccess {
				stopLatch.countDown()
			}
			stopLatch.await(5, TimeUnit.SECONDS)
	}

}