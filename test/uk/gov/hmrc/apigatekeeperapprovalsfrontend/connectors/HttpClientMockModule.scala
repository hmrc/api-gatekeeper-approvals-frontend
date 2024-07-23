/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apigatekeeperapprovalsfrontend.connectors

import java.net.URL
import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

trait HttpClientMockModule extends MockitoSugar with ArgumentMatchersSugar {

  trait BaseHttpClientMockModule {
    def aMock: HttpClientV2
    protected def requestBuilderMock: RequestBuilder

    object Get {

      def thenReturn[T](response: T) = {
        when(aMock.get(*)(*)).thenReturn(requestBuilderMock)
        when(requestBuilderMock.execute[T](*, *)).thenReturn(Future.successful(response))
      }

      def verifyUrl(url: URL) = {
        verify(aMock).get(eqTo(url))(*)
      }
    }
  }

  object HttpClientMock extends BaseHttpClientMockModule {
    val aMock: HttpClientV2                          = mock[HttpClientV2]
    protected val requestBuilderMock: RequestBuilder = mock[RequestBuilder]
  }
}
